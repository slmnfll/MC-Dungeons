package me.slmnfll.dungeons.encounter;

import me.slmnfll.dungeons.content.BlockPosition;
import me.slmnfll.dungeons.content.DungeonItemDeliveryService;
import me.slmnfll.dungeons.content.DungeonLootService;
import me.slmnfll.dungeons.content.DungeonRiskService;
import me.slmnfll.dungeons.instance.DungeonInstanceManager;
import me.slmnfll.dungeons.instance.DungeonInstanceState;
import me.slmnfll.dungeons.instance.DungeonSession;
import me.slmnfll.dungeons.instance.DungeonRunType;
import me.slmnfll.dungeons.layout.RoomNode;
import me.slmnfll.dungeons.profile.DungeonProfileService;
import me.slmnfll.dungeons.world.DungeonGeometry;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Warden;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

public final class DungeonEncounterService implements Listener {
    private static final EntityType[] TEST_ENEMIES = {
            EntityType.ZOMBIE,
            EntityType.SKELETON,
            EntityType.SPIDER
    };
    private static final double TUTORIAL_WARDEN_HEALTH = 80.0D;
    private static final double TUTORIAL_WARDEN_DAMAGE = 6.0D;

    private final JavaPlugin plugin;
    private final DungeonInstanceManager instanceManager;
    private final DungeonProfileService profileService;
    private final DungeonLootService lootService;
    private final DungeonItemDeliveryService itemDeliveryService;
    private final DungeonGateService gateService;
    private final DungeonRoomLocator roomLocator;
    private final DungeonExitPortalService exitPortalService;
    private final DungeonCombatTuning tuning;
    private final DungeonRiskService riskService;
    private final NamespacedKey instanceKey;
    private final NamespacedKey roomKey;
    private final NamespacedKey enemyLevelKey;
    private final NamespacedKey bossKey;
    private final Map<UUID, BossDisplay> bossDisplays = new HashMap<>();
    private BukkitTask maintenanceTask;

    public DungeonEncounterService(
            JavaPlugin plugin,
            DungeonInstanceManager instanceManager,
            DungeonProfileService profileService,
            DungeonLootService lootService,
            DungeonItemDeliveryService itemDeliveryService,
            DungeonGateService gateService,
            DungeonRoomLocator roomLocator,
            DungeonExitPortalService exitPortalService,
            DungeonCombatTuning tuning,
            DungeonRiskService riskService
    ) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.instanceManager = Objects.requireNonNull(instanceManager, "instanceManager");
        this.profileService = Objects.requireNonNull(profileService, "profileService");
        this.lootService = Objects.requireNonNull(lootService, "lootService");
        this.itemDeliveryService = Objects.requireNonNull(itemDeliveryService, "itemDeliveryService");
        this.gateService = Objects.requireNonNull(gateService, "gateService");
        this.roomLocator = Objects.requireNonNull(roomLocator, "roomLocator");
        this.exitPortalService = Objects.requireNonNull(exitPortalService, "exitPortalService");
        this.tuning = Objects.requireNonNull(tuning, "tuning");
        this.riskService = Objects.requireNonNull(riskService, "riskService");
        this.instanceKey = new NamespacedKey(plugin, "encounter_instance");
        this.roomKey = new NamespacedKey(plugin, "encounter_room");
        this.enemyLevelKey = new NamespacedKey(plugin, "enemy_level");
        this.bossKey = new NamespacedKey(plugin, "encounter_boss");
    }

    public void start() {
        maintenanceTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 10L, 10L);
    }

    public void stop() {
        if (maintenanceTask != null) {
            maintenanceTask.cancel();
        }
        bossDisplays.values().forEach(display -> display.bossBar().removeAll());
        bossDisplays.clear();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (event.getFrom().getBlockX() == event.getTo().getBlockX()
                && event.getFrom().getBlockY() == event.getTo().getBlockY()
                && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }

        Player player = event.getPlayer();
        DungeonSession session = instanceManager.sessionFor(player).orElse(null);
        if (session == null || !instanceManager.isInDungeon(player)) {
            return;
        }

        BlockPosition destination = new BlockPosition(
                event.getTo().getBlockX(),
                event.getTo().getBlockY(),
                event.getTo().getBlockZ()
        );
        if (session.encounters().exitPortalBlocks().contains(destination)) {
            player.sendMessage(Component.text("Your expedition loot is secured.", NamedTextColor.GREEN));
            instanceManager.secureExit(player);
            return;
        }

        RoomNode room = roomLocator.locate(session.instance().layout(), event.getTo()).orElse(null);
        if (room == null) {
            return;
        }
        RoomEncounter encounter = session.encounters().room(room.id()).orElse(null);
        if (encounter == null || encounter.state() != EncounterRoomState.PENDING) {
            return;
        }

        if (encounter.definition().boss() && !session.encounters().bossUnlocked()) {
            event.setCancelled(true);
            player.sendMessage(Component.text(
                    bossName(session) + " remains sealed. Clear the main-route encounters first.",
                    NamedTextColor.RED
            ));
            return;
        }
        if (session.encounters().activeRoom().isPresent()) {
            event.setCancelled(true);
            return;
        }
        activate(session, player, encounter);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEncounterMobDeath(EntityDeathEvent event) {
        String instanceIdValue = event.getEntity().getPersistentDataContainer()
                .get(instanceKey, PersistentDataType.STRING);
        Integer roomId = event.getEntity().getPersistentDataContainer()
                .get(roomKey, PersistentDataType.INTEGER);
        Integer enemyLevel = event.getEntity().getPersistentDataContainer()
                .get(enemyLevelKey, PersistentDataType.INTEGER);
        if (instanceIdValue == null || roomId == null || enemyLevel == null) {
            return;
        }

        DungeonSession session = instanceManager.sessionFor(event.getEntity().getWorld()).orElse(null);
        if (session == null || !session.instance().id().toString().equals(instanceIdValue)) {
            return;
        }
        RoomEncounter encounter = session.encounters().room(roomId).orElse(null);
        if (encounter == null || !encounter.removeMob(event.getEntity().getUniqueId())) {
            return;
        }

        event.getDrops().clear();
        event.setDroppedExp(0);
        Player player = Bukkit.getPlayer(session.instance().ownerId());
        if (player != null) {
            profileService.award(player, tuning.killExperience(enemyLevel, encounter.definition().boss()), false);
        }

        if (encounter.livingMobCount() == 0) {
            if (encounter.hasAnotherWave()) {
                scheduleWave(session, encounter, 35L);
            } else {
                clearEncounter(session, encounter);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEncounterProjectileLaunch(ProjectileLaunchEvent event) {
        if (!(event.getEntity().getShooter() instanceof LivingEntity shooter)) {
            return;
        }
        String instanceId = shooter.getPersistentDataContainer().get(instanceKey, PersistentDataType.STRING);
        Integer enemyLevel = shooter.getPersistentDataContainer().get(enemyLevelKey, PersistentDataType.INTEGER);
        Byte boss = shooter.getPersistentDataContainer().get(bossKey, PersistentDataType.BYTE);
        if (instanceId == null || enemyLevel == null || boss == null) {
            return;
        }
        event.getEntity().getPersistentDataContainer().set(instanceKey, PersistentDataType.STRING, instanceId);
        event.getEntity().getPersistentDataContainer().set(enemyLevelKey, PersistentDataType.INTEGER, enemyLevel);
        event.getEntity().getPersistentDataContainer().set(bossKey, PersistentDataType.BYTE, boss);
    }

    @EventHandler(ignoreCancelled = true)
    public void onEncounterProjectileDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Projectile projectile)) {
            return;
        }
        Integer enemyLevel = projectile.getPersistentDataContainer().get(enemyLevelKey, PersistentDataType.INTEGER);
        Byte boss = projectile.getPersistentDataContainer().get(bossKey, PersistentDataType.BYTE);
        if (enemyLevel != null && boss != null) {
            event.setDamage(event.getDamage() * tuning.damageMultiplier(enemyLevel, boss == (byte) 1));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onTutorialWardenDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof LivingEntity attacker)) {
            return;
        }
        Byte boss = attacker.getPersistentDataContainer().get(bossKey, PersistentDataType.BYTE);
        String instanceId = attacker.getPersistentDataContainer().get(instanceKey, PersistentDataType.STRING);
        if (boss == null || boss != (byte) 1 || instanceId == null) {
            return;
        }
        DungeonSession session = instanceManager.sessionFor(attacker.getWorld()).orElse(null);
        if (session != null
                && session.instance().runType() == DungeonRunType.TUTORIAL
                && session.instance().id().toString().equals(instanceId)) {
            event.setDamage(Math.min(event.getDamage(), TUTORIAL_WARDEN_DAMAGE));
        }
    }

    private void activate(DungeonSession session, Player player, RoomEncounter encounter) {
        encounter.activate();
        Material gateMaterial = encounter.definition().boss()
                ? Material.PURPLE_STAINED_GLASS
                : Material.RED_STAINED_GLASS;
        gateService.seal(session.world(), session.instance().layout(), encounter.definition().roomId(), gateMaterial);
        player.sendMessage(Component.text(
                encounter.definition().boss() ? bossName(session) + " awakens." : "The room seals around you.",
                encounter.definition().boss() ? NamedTextColor.DARK_RED : NamedTextColor.RED
        ));
        player.playSound(player.getLocation(), Sound.BLOCK_IRON_DOOR_CLOSE, 1.0F, 0.75F);
        scheduleWave(session, encounter, 15L);
    }

    private void scheduleWave(DungeonSession session, RoomEncounter encounter, long delayTicks) {
        encounter.scheduleWave();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            DungeonSession currentSession = instanceManager.sessionFor(session.world()).orElse(null);
            if (currentSession == null
                    || !currentSession.instance().id().equals(session.instance().id())
                    || currentSession.instance().state() != DungeonInstanceState.ACTIVE
                    || encounter.state() != EncounterRoomState.ACTIVE
                    || !encounter.waveScheduled()) {
                return;
            }
            encounter.beginScheduledWave();
            spawnWave(currentSession, encounter);
        }, delayTicks);
    }

    private void spawnWave(DungeonSession session, RoomEncounter encounter) {
        Player player = Bukkit.getPlayer(session.instance().ownerId());
        if (player == null || !player.isOnline()) {
            return;
        }

        boolean tutorial = session.instance().runType() == DungeonRunType.TUTORIAL;
        int playerLevel = profileService.effectiveLevel(player);
        EncounterDefinition definition = encounter.definition();
        int enemyLevel = tutorial
                ? (definition.boss() ? 3 : 1)
                : tuning.enemyLevel(playerLevel, definition.depth(), definition.boss());
        int mobCount = tutorial
                ? (definition.boss() ? 1 : 2)
                : (definition.boss()
                        ? 1
                        : tuning.normalMobCount(playerLevel, definition.depth(), encounter.currentWave()));
        Random random = new Random(
                session.instance().seed()
                        ^ ((long) definition.roomId() << 32)
                        ^ encounter.currentWave()
        );

        for (int index = 0; index < mobCount; index++) {
            EntityType entityType;
            if (tutorial && definition.boss()) {
                entityType = EntityType.WARDEN;
            } else if (definition.boss()) {
                entityType = EntityType.WITHER_SKELETON;
            } else if (tutorial) {
                entityType = index == 0 ? EntityType.ZOMBIE : EntityType.SKELETON;
            } else {
                entityType = TEST_ENEMIES[random.nextInt(TEST_ENEMIES.length)];
            }
            Mob mob = spawnMob(session, player, definition, entityType, enemyLevel, random, index);
            encounter.addMob(mob.getUniqueId());
            if (definition.boss()) {
                showBossBar(session, player, mob);
            }
        }

        player.sendMessage(Component.text(
                definition.boss()
                        ? "Defeat " + bossName(session) + "."
                        : "Wave " + encounter.currentWave() + "/" + definition.waveCount(),
                NamedTextColor.RED
        ));
    }

    private Mob spawnMob(
            DungeonSession session,
            Player player,
            EncounterDefinition encounter,
            EntityType entityType,
            int enemyLevel,
            Random random,
            int index
    ) {
        RoomNode room = session.instance().layout().roomById(encounter.roomId()).orElseThrow();
        int centerX = DungeonGeometry.worldCoordinate(room.position().x());
        int centerZ = DungeonGeometry.worldCoordinate(room.position().z());
        int offsetX = -3 + Math.floorMod(index * 3 + random.nextInt(3), 7);
        int offsetZ = -3 + Math.floorMod(index * 5 + random.nextInt(3), 7);
        Location spawn = new Location(
                session.world(),
                centerX + offsetX + 0.5D,
                DungeonGeometry.FLOOR_Y + 1.0D,
                centerZ + offsetZ + 0.5D
        );
        Entity entity = session.world().spawnEntity(spawn, entityType, CreatureSpawnEvent.SpawnReason.CUSTOM);
        if (!(entity instanceof Mob mob)) {
            entity.remove();
            throw new IllegalStateException("Encounter entity is not a Mob: " + entityType);
        }

        mob.getPersistentDataContainer().set(instanceKey, PersistentDataType.STRING, session.instance().id().toString());
        mob.getPersistentDataContainer().set(roomKey, PersistentDataType.INTEGER, encounter.roomId());
        mob.getPersistentDataContainer().set(enemyLevelKey, PersistentDataType.INTEGER, enemyLevel);
        mob.getPersistentDataContainer().set(bossKey, PersistentDataType.BYTE, (byte) (encounter.boss() ? 1 : 0));
        mob.setPersistent(true);
        mob.setRemoveWhenFarAway(false);
        mob.setCanPickupItems(false);
        mob.setTarget(player);
        if (mob instanceof Warden warden) {
            warden.setAnger(player, 150);
            warden.setDisturbanceLocation(player.getLocation());
        }

        boolean tutorialBoss = session.instance().runType() == DungeonRunType.TUTORIAL && encounter.boss();
        AttributeInstance health = mob.getAttribute(Attribute.MAX_HEALTH);
        if (health != null) {
            double maximumHealth = tutorialBoss
                    ? TUTORIAL_WARDEN_HEALTH
                    : Math.min(
                            2_048.0D,
                            health.getBaseValue() * tuning.healthMultiplier(enemyLevel, encounter.boss())
                    );
            health.setBaseValue(maximumHealth);
            mob.setHealth(maximumHealth);
        }
        AttributeInstance damage = mob.getAttribute(Attribute.ATTACK_DAMAGE);
        if (damage != null) {
            damage.setBaseValue(tutorialBoss
                    ? TUTORIAL_WARDEN_DAMAGE
                    : Math.min(
                            100.0D,
                            damage.getBaseValue() * tuning.damageMultiplier(enemyLevel, encounter.boss())
                    ));
        }

        String typeName = switch (entityType) {
            case ZOMBIE -> "Zombie";
            case SKELETON -> "Skeleton";
            case SPIDER -> "Spider";
            case WITHER_SKELETON -> "Crypt Warden";
            case WARDEN -> "Aevori Survey Warden";
            default -> "Dungeon Enemy";
        };
        mob.customName(Component.text(
                typeName + "  Lv. " + enemyLevel,
                encounter.boss() ? NamedTextColor.DARK_RED : NamedTextColor.RED
        ).decorate(TextDecoration.BOLD));
        mob.setCustomNameVisible(true);
        if (encounter.boss()) {
            mob.getEquipment().setItemInMainHand(new ItemStack(Material.NETHERITE_SWORD));
        }
        session.world().spawnParticle(Particle.LARGE_SMOKE, spawn, 10, 0.3D, 0.3D, 0.3D, 0.01D);
        return mob;
    }

    private void clearEncounter(DungeonSession session, RoomEncounter encounter) {
        encounter.clear();
        gateService.open(session.world(), session.instance().layout(), encounter.definition().roomId());
        Player player = Bukkit.getPlayer(session.instance().ownerId());
        if (player == null) {
            return;
        }

        int playerLevel = profileService.effectiveLevel(player);
        profileService.award(player, tuning.roomExperience(encounter.definition(), playerLevel));
        long rewardSeed = session.instance().seed() ^ ((long) encounter.definition().roomId() << 24) ^ 0x5EEDL;
        List<ItemStack> encounterLoot = session.instance().runType() == DungeonRunType.TUTORIAL
                ? lootService.tutorialEncounterLoot(rewardSeed, encounter.definition().boss())
                : lootService.rollEncounterLoot(
                        rewardSeed,
                        !encounter.definition().required(),
                        encounter.definition().boss(),
                        session.instance().affinity(),
                        playerLevel
                );
        itemDeliveryService.give(player, riskService.markAtRisk(encounterLoot, session.instance().id()));

        if (encounter.definition().boss()) {
            hideBossBar(session.instance().id());
            instanceManager.markBossDefeated(player);
            Set<BlockPosition> portalBlocks = exitPortalService.create(session.world(), session.instance().layout());
            session.encounters().beginExitCountdown(
                    System.currentTimeMillis() + DungeonCombatTuning.EXIT_TIME_MILLIS,
                    portalBlocks
            );
            player.sendMessage(Component.text(
                    bossName(session) + " falls. Reach the exit portal within five minutes!",
                    NamedTextColor.AQUA
            ));
            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0F, 0.9F);
            return;
        }

        player.sendMessage(Component.text(
                encounter.definition().required()
                        ? "Required encounter cleared."
                        : "Optional encounter cleared — bonus rewards earned.",
                NamedTextColor.GREEN
        ));
        player.playSound(player.getLocation(), Sound.BLOCK_IRON_DOOR_OPEN, 1.0F, 1.1F);
        if (session.encounters().refreshBossUnlock()) {
            gateService.open(session.world(), session.instance().layout(), session.instance().layout().bossRoomId());
            player.sendMessage(Component.text(
                    "A distant seal breaks. The Crypt Warden can now be challenged.",
                    NamedTextColor.DARK_PURPLE
            ));
        }
    }

    private void showBossBar(DungeonSession session, Player player, LivingEntity boss) {
        hideBossBar(session.instance().id());
        BossBar bossBar = Bukkit.createBossBar(bossName(session), BarColor.RED, BarStyle.SOLID);
        bossBar.addPlayer(player);
        bossDisplays.put(session.instance().id(), new BossDisplay(boss.getUniqueId(), bossBar));
    }

    private void hideBossBar(UUID instanceId) {
        BossDisplay display = bossDisplays.remove(instanceId);
        if (display != null) {
            display.bossBar().removeAll();
        }
    }

    private String bossName(DungeonSession session) {
        return session.instance().runType() == DungeonRunType.TUTORIAL
                ? "Aevori Survey Warden"
                : "Crypt Warden";
    }

    private void tick() {
        Set<UUID> activeInstanceIds = new HashSet<>();
        for (DungeonSession session : instanceManager.activeSessions()) {
            activeInstanceIds.add(session.instance().id());
            updateBossBar(session);
            if (session.instance().state() != DungeonInstanceState.BOSS_DEFEATED) {
                continue;
            }
            if (session.encounters().exitDeadlineMillis() > 0L
                    && System.currentTimeMillis() >= session.encounters().exitDeadlineMillis()) {
                Player player = Bukkit.getPlayer(session.instance().ownerId());
                if (player != null && player.isOnline()) {
                    instanceManager.failAndEject(player, "A strong wind expelled you before you reached the exit.");
                }
            }
        }

        Iterator<Map.Entry<UUID, BossDisplay>> iterator = bossDisplays.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, BossDisplay> entry = iterator.next();
            if (!activeInstanceIds.contains(entry.getKey())) {
                entry.getValue().bossBar().removeAll();
                iterator.remove();
            }
        }
    }

    private void updateBossBar(DungeonSession session) {
        BossDisplay display = bossDisplays.get(session.instance().id());
        if (display == null) {
            return;
        }
        Entity entity = session.world().getEntity(display.entityId());
        if (!(entity instanceof LivingEntity boss) || boss.isDead()) {
            return;
        }
        AttributeInstance maximumHealth = boss.getAttribute(Attribute.MAX_HEALTH);
        if (maximumHealth != null && maximumHealth.getValue() > 0.0D) {
            display.bossBar().setProgress(Math.clamp(boss.getHealth() / maximumHealth.getValue(), 0.0D, 1.0D));
        }
    }

    private record BossDisplay(UUID entityId, BossBar bossBar) {
    }
}
