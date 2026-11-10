package me.slmnfll.dungeons.tutorial;

import me.slmnfll.dungeons.encounter.EncounterRoomState;
import me.slmnfll.dungeons.encounter.DungeonRoomLocator;
import me.slmnfll.dungeons.instance.DungeonInstanceManager;
import me.slmnfll.dungeons.instance.DungeonInstanceState;
import me.slmnfll.dungeons.instance.DungeonRunClosedEvent;
import me.slmnfll.dungeons.instance.DungeonRunStartedEvent;
import me.slmnfll.dungeons.instance.DungeonRunType;
import me.slmnfll.dungeons.instance.DungeonSession;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class TutorialObjectiveService implements Listener {
    private final JavaPlugin plugin;
    private final DungeonInstanceManager instanceManager;
    private final TutorialObjectivePolicy policy = new TutorialObjectivePolicy();
    private final DungeonRoomLocator roomLocator = new DungeonRoomLocator();
    private final Map<UUID, ActiveObjective> activeObjectives = new HashMap<>();
    private BukkitTask task;

    public TutorialObjectiveService(JavaPlugin plugin, DungeonInstanceManager instanceManager) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.instanceManager = Objects.requireNonNull(instanceManager, "instanceManager");
    }

    public void start() {
        if (task == null) {
            task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 1L, 5L);
        }
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        activeObjectives.values().forEach(objective -> {
            Player player = Bukkit.getPlayer(objective.playerId());
            if (player != null) {
                player.hideBossBar(objective.bar());
            }
        });
        activeObjectives.clear();
    }

    public boolean canBreakPot(Player player, DungeonSession session) {
        if (session.instance().runType() != DungeonRunType.TUTORIAL) {
            return true;
        }
        ActiveObjective objective = activeObjectives.get(session.instance().id());
        if (objective == null) {
            return false;
        }
        update(player, session, objective);
        return objective.stage() != TutorialObjectiveStage.REACH_POT_ROOM;
    }

    @EventHandler
    public void onRunStarted(DungeonRunStartedEvent event) {
        if (event.session().instance().runType() != DungeonRunType.TUTORIAL) {
            return;
        }
        BossBar bar = BossBar.bossBar(
                Component.empty(),
                0.0F,
                BossBar.Color.PURPLE,
                BossBar.Overlay.PROGRESS
        );
        ActiveObjective objective = new ActiveObjective(event.player().getUniqueId(), bar, null);
        activeObjectives.put(event.session().instance().id(), objective);
        event.player().showBossBar(bar);
        update(event.player(), event.session(), objective);
    }

    @EventHandler
    public void onRunClosed(DungeonRunClosedEvent event) {
        ActiveObjective objective = activeObjectives.remove(event.instanceId());
        if (objective != null) {
            event.player().hideBossBar(objective.bar());
        }
    }

    private void tick() {
        for (DungeonSession session : instanceManager.activeSessions()) {
            if (session.instance().runType() != DungeonRunType.TUTORIAL) {
                continue;
            }
            ActiveObjective objective = activeObjectives.get(session.instance().id());
            Player player = Bukkit.getPlayer(session.instance().ownerId());
            if (objective != null && player != null && player.isOnline() && instanceManager.isInDungeon(player)) {
                update(player, session, objective);
            }
        }
    }

    private void update(Player player, DungeonSession session, ActiveObjective objective) {
        EncounterRoomState requiredState = session.encounters()
                .room(SurveySiteZeroDefinition.COMBAT_ROOM_ID)
                .orElseThrow()
                .state();
        EncounterRoomState bossState = session.encounters()
                .room(SurveySiteZeroDefinition.BOSS_ROOM_ID)
                .orElseThrow()
                .state();
        TutorialObjectiveStage stage = policy.stage(new TutorialObjectivePolicy.Snapshot(
                roomLocator.locate(session.instance().layout(), player.getLocation())
                        .filter(room -> room.id() == 1)
                        .isPresent(),
                session.progress().brokenPots(),
                session.progress().keys(),
                session.progress().openedChests(),
                requiredState == EncounterRoomState.CLEARED,
                bossState == EncounterRoomState.ACTIVE,
                session.instance().state() == DungeonInstanceState.BOSS_DEFEATED
        ));
        if (stage == objective.stage()) {
            return;
        }
        objective.stage(stage);
        objective.bar().name(Component.text("Tutorial: " + stage.objective(), NamedTextColor.GOLD));
        objective.bar().progress((stage.ordinal() + 1.0F) / TutorialObjectiveStage.values().length);
        player.sendMessage(Component.text("Tutorial — ", NamedTextColor.GOLD)
                .append(Component.text(stage.objective(), NamedTextColor.YELLOW)));
        player.sendMessage(Component.text(stage.guidance(), NamedTextColor.GRAY));
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.65F, 1.35F);
    }

    private static final class ActiveObjective {
        private final UUID playerId;
        private final BossBar bar;
        private TutorialObjectiveStage stage;

        private ActiveObjective(UUID playerId, BossBar bar, TutorialObjectiveStage stage) {
            this.playerId = playerId;
            this.bar = bar;
            this.stage = stage;
        }

        UUID playerId() {
            return playerId;
        }

        BossBar bar() {
            return bar;
        }

        TutorialObjectiveStage stage() {
            return stage;
        }

        void stage(TutorialObjectiveStage stage) {
            this.stage = stage;
        }
    }
}
