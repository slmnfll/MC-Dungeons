package me.slmnfll.dungeons.instance;

import me.slmnfll.dungeons.affinity.Affinity;
import me.slmnfll.dungeons.content.DungeonRunProgress;
import me.slmnfll.dungeons.content.DungeonDeathLootResult;
import me.slmnfll.dungeons.content.DungeonRiskService;
import me.slmnfll.dungeons.equipment.DungeonEquipmentService;
import me.slmnfll.dungeons.layout.DungeonLayout;
import me.slmnfll.dungeons.layout.DungeonLayoutPlanner;
import me.slmnfll.dungeons.layout.DungeonLayoutValidator;
import me.slmnfll.dungeons.map.ExpeditionMapService;
import me.slmnfll.dungeons.profile.DungeonProfileService;
import me.slmnfll.dungeons.world.DungeonWorldService;
import me.slmnfll.dungeons.tutorial.SurveySiteZeroDefinition;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.logging.Level;

public final class DungeonInstanceManager {
    private final JavaPlugin plugin;
    private final DungeonLayoutPlanner layoutPlanner;
    private final DungeonLayoutValidator layoutValidator;
    private final DungeonWorldService worldService;
    private final ExpeditionMapService mapService;
    private final DungeonRiskService riskService;
    private final DungeonEquipmentService equipmentService;
    private final DungeonProfileService profileService;
    private final SurveySiteZeroDefinition tutorialDefinition;
    private final Map<UUID, DungeonSession> sessionsByOwner = new HashMap<>();
    private final Map<UUID, DungeonSession> sessionsByWorld = new HashMap<>();
    private Predicate<Player> adventureAccess = ignored -> false;

    public DungeonInstanceManager(
            JavaPlugin plugin,
            DungeonLayoutPlanner layoutPlanner,
            DungeonLayoutValidator layoutValidator,
            DungeonWorldService worldService,
            ExpeditionMapService mapService,
            DungeonRiskService riskService,
            DungeonEquipmentService equipmentService,
            DungeonProfileService profileService,
            SurveySiteZeroDefinition tutorialDefinition
    ) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.layoutPlanner = Objects.requireNonNull(layoutPlanner, "layoutPlanner");
        this.layoutValidator = Objects.requireNonNull(layoutValidator, "layoutValidator");
        this.worldService = Objects.requireNonNull(worldService, "worldService");
        this.mapService = Objects.requireNonNull(mapService, "mapService");
        this.riskService = Objects.requireNonNull(riskService, "riskService");
        this.equipmentService = Objects.requireNonNull(equipmentService, "equipmentService");
        this.profileService = Objects.requireNonNull(profileService, "profileService");
        this.tutorialDefinition = Objects.requireNonNull(tutorialDefinition, "tutorialDefinition");
    }

    public DungeonSession start(Player player, Affinity affinity, long seed) {
        return start(player, affinity, seed, DungeonRunType.ADVENTURE);
    }

    public DungeonSession startTutorial(Player player, Affinity affinity) {
        return start(player, affinity, SurveySiteZeroDefinition.SEED, DungeonRunType.TUTORIAL);
    }

    public void setAdventureAccess(Predicate<Player> adventureAccess) {
        this.adventureAccess = Objects.requireNonNull(adventureAccess, "adventureAccess");
    }

    private DungeonSession start(Player player, Affinity affinity, long seed, DungeonRunType runType) {
        requirePrimaryThread();
        if (sessionsByOwner.containsKey(player.getUniqueId())) {
            throw new IllegalStateException("You already have an active dungeon instance.");
        }
        if (runType == DungeonRunType.ADVENTURE) {
            if (!adventureAccess.test(player)) {
                throw new IllegalStateException(
                        "Complete Survey Site Zero with the Guild Chronicler before beginning an adventure dungeon."
                );
            }
            equipmentService.validateEntry(player);
        } else {
            equipmentService.validateTutorialEntry(player);
        }
        if (!mapService.canGive(player)) {
            throw new IllegalStateException("You need one free inventory slot for the expedition map.");
        }

        DungeonLayout layout = runType == DungeonRunType.TUTORIAL
                ? tutorialDefinition.layout()
                : layoutPlanner.plan(seed);
        layoutValidator.validate(layout);
        DungeonInstance instance = new DungeonInstance(
                UUID.randomUUID(),
                player.getUniqueId(),
                affinity,
                layout,
                runType
        );
        Location returnLocation = player.getLocation().clone();
        DungeonPlayerState playerState = DungeonPlayerState.capture(player);
        DungeonWorldService.CreatedDungeonWorld createdWorld = null;

        try {
            instance.transitionTo(DungeonInstanceState.GENERATING);
            createdWorld = worldService.create(instance);
            mapService.give(player, createdWorld.world(), instance);

            DungeonSession session = new DungeonSession(
                    instance,
                    createdWorld.world(),
                    returnLocation,
                    playerState,
                    createdWorld.content(),
                    new DungeonRunProgress(createdWorld.content()),
                    createdWorld.encounters()
            );
            sessionsByOwner.put(player.getUniqueId(), session);
            sessionsByWorld.put(createdWorld.world().getUID(), session);

            if (!player.teleport(createdWorld.spawn())) {
                throw new IllegalStateException("Paper refused to teleport you into the dungeon.");
            }
            playerState.prepareForDungeon(player);
            instance.transitionTo(DungeonInstanceState.ACTIVE);
            Bukkit.getPluginManager().callEvent(new DungeonRunStartedEvent(player, session));
            return session;
        } catch (RuntimeException exception) {
            sessionsByOwner.remove(player.getUniqueId());
            if (createdWorld != null) {
                sessionsByWorld.remove(createdWorld.world().getUID());
            }
            mapService.remove(player);
            if (player.getWorld().getName().equals(instance.worldName())) {
                player.teleport(returnLocation);
            }
            playerState.restore(player);
            if (createdWorld != null && createdWorld.world().getPlayers().isEmpty()) {
                try {
                    worldService.destroy(createdWorld.world());
                } catch (RuntimeException cleanupException) {
                    exception.addSuppressed(cleanupException);
                }
            }
            if (instance.state().canTransitionTo(DungeonInstanceState.FAILED)) {
                instance.transitionTo(DungeonInstanceState.FAILED);
            }
            throw exception;
        }
    }

    public Optional<DungeonSession> sessionFor(Player player) {
        return Optional.ofNullable(sessionsByOwner.get(player.getUniqueId()));
    }

    public Optional<DungeonSession> sessionFor(World world) {
        return Optional.ofNullable(sessionsByWorld.get(world.getUID()));
    }

    public boolean isInDungeon(Player player) {
        DungeonSession session = sessionsByOwner.get(player.getUniqueId());
        return session != null && player.getWorld().getUID().equals(session.world().getUID());
    }

    public java.util.List<DungeonSession> activeSessions() {
        return java.util.List.copyOf(sessionsByOwner.values());
    }

    public void leave(Player player) {
        requirePrimaryThread();
        DungeonSession session = requireSession(player);
        closeSession(session, player, true, DungeonRunEndReason.LEFT);
    }

    public void complete(Player player) {
        requirePrimaryThread();
        DungeonSession session = requireSession(player);
        DungeonInstance instance = session.instance();
        if (instance.state() != DungeonInstanceState.ACTIVE) {
            throw new IllegalStateException("That dungeon is not active.");
        }
        instance.transitionTo(DungeonInstanceState.BOSS_DEFEATED);
        instance.transitionTo(DungeonInstanceState.COMPLETED);
        if (instance.runType() == DungeonRunType.ADVENTURE) {
            profileService.recordCompletedExpedition(player);
        }
        closeSession(session, player, true, DungeonRunEndReason.COMPLETED);
    }

    public void markBossDefeated(Player player) {
        requirePrimaryThread();
        DungeonSession session = requireSession(player);
        if (session.instance().state() != DungeonInstanceState.ACTIVE) {
            throw new IllegalStateException("The boss cannot be defeated in the current run state.");
        }
        session.instance().transitionTo(DungeonInstanceState.BOSS_DEFEATED);
    }

    public void secureExit(Player player) {
        requirePrimaryThread();
        DungeonSession session = requireSession(player);
        if (session.instance().state() != DungeonInstanceState.BOSS_DEFEATED) {
            throw new IllegalStateException("The dungeon exit is not available yet.");
        }
        session.instance().transitionTo(DungeonInstanceState.COMPLETED);
        if (session.instance().runType() == DungeonRunType.ADVENTURE) {
            profileService.recordCompletedExpedition(player);
        }
        closeSession(session, player, true, DungeonRunEndReason.COMPLETED);
    }

    public void failAndEject(Player player, String message) {
        requirePrimaryThread();
        DungeonSession session = requireSession(player);
        DungeonInstanceState state = session.instance().state();
        if (state != DungeonInstanceState.ACTIVE && state != DungeonInstanceState.BOSS_DEFEATED) {
            return;
        }
        session.instance().transitionTo(DungeonInstanceState.FAILED);
        player.sendMessage(message);
        closeSession(session, player, true, DungeonRunEndReason.EJECTED);
    }

    public Optional<DungeonDeathLootResult> markFailedByDeath(Player player) {
        requirePrimaryThread();
        DungeonSession session = sessionsByOwner.get(player.getUniqueId());
        if (session == null || (session.instance().state() != DungeonInstanceState.ACTIVE
                && session.instance().state() != DungeonInstanceState.BOSS_DEFEATED)) {
            return Optional.empty();
        }
        session.instance().transitionTo(DungeonInstanceState.FAILED);
        return Optional.of(session.instance().runType() == DungeonRunType.TUTORIAL
                ? riskService.resolveTotalLoss(player, session.instance().id())
                : riskService.resolveDeath(player, session.instance().id()));
    }

    public Optional<Location> failedRunRespawnLocation(Player player) {
        DungeonSession session = sessionsByOwner.get(player.getUniqueId());
        if (session == null || session.instance().state() != DungeonInstanceState.FAILED) {
            return Optional.empty();
        }
        return Optional.of(session.returnLocation());
    }

    public void finishFailedRun(Player player) {
        requirePrimaryThread();
        DungeonSession session = sessionsByOwner.get(player.getUniqueId());
        if (session != null && session.instance().state() == DungeonInstanceState.FAILED) {
            closeSession(session, player, false, DungeonRunEndReason.DEATH);
        }
    }

    public void handleDisconnect(Player player) {
        requirePrimaryThread();
        DungeonSession session = sessionsByOwner.get(player.getUniqueId());
        if (session != null) {
            closeSession(
                    session,
                    player,
                    session.instance().state() != DungeonInstanceState.FAILED,
                    session.instance().state() == DungeonInstanceState.FAILED
                            ? DungeonRunEndReason.DEATH
                            : DungeonRunEndReason.DISCONNECTED
            );
        }
    }

    public void handleExternalExit(Player player) {
        requirePrimaryThread();
        DungeonSession session = sessionsByOwner.get(player.getUniqueId());
        if (session != null && !player.getWorld().getUID().equals(session.world().getUID())) {
            closeSession(session, player, false, DungeonRunEndReason.EXTERNAL_EXIT);
        }
    }

    public void shutdown() {
        requirePrimaryThread();
        for (DungeonSession session : new ArrayList<>(sessionsByOwner.values())) {
            Player player = Bukkit.getPlayer(session.instance().ownerId());
            try {
                closeSession(
                        session,
                        player,
                        player != null && player.isOnline(),
                        DungeonRunEndReason.SERVER_SHUTDOWN
                );
            } catch (RuntimeException exception) {
                plugin.getLogger().log(Level.SEVERE, "Could not close dungeon " + session.instance().id(), exception);
            }
        }
    }

    private DungeonSession requireSession(Player player) {
        DungeonSession session = sessionsByOwner.get(player.getUniqueId());
        if (session == null) {
            throw new IllegalStateException("You do not have an active dungeon instance.");
        }
        return session;
    }

    private void closeSession(
            DungeonSession session,
            Player player,
            boolean teleportPlayer,
            DungeonRunEndReason endReason
    ) {
        DungeonInstance instance = session.instance();
        if (instance.state() == DungeonInstanceState.CLOSED || instance.state() == DungeonInstanceState.CLOSING) {
            return;
        }

        DungeonInstanceState finalState = instance.state();
        if (player != null) {
            if (instance.state() == DungeonInstanceState.COMPLETED) {
                riskService.secure(player, instance.id());
            } else {
                riskService.discard(player, instance.id());
            }
            if (teleportPlayer && player.getWorld().getUID().equals(session.world().getUID())) {
                if (!player.teleport(session.returnLocation())) {
                    throw new IllegalStateException("Could not return the player before closing the dungeon.");
                }
            }
            session.playerState().restore(player);
            mapService.remove(player);
        }

        instance.transitionTo(DungeonInstanceState.CLOSING);
        sessionsByOwner.remove(instance.ownerId());
        sessionsByWorld.remove(session.world().getUID());
        worldService.destroy(session.world());
        instance.transitionTo(DungeonInstanceState.CLOSED);
        if (player != null) {
            Bukkit.getPluginManager().callEvent(new DungeonRunClosedEvent(
                    player,
                    finalState,
                    instance.id(),
                    instance.runType(),
                    endReason
            ));
        }
    }

    private void requirePrimaryThread() {
        if (!Bukkit.isPrimaryThread()) {
            throw new IllegalStateException("Dungeon lifecycle operations must run on the server thread.");
        }
    }
}
