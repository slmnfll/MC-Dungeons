package me.slmnfll.dungeons.content;

import me.slmnfll.dungeons.instance.DungeonInstanceManager;
import me.slmnfll.dungeons.instance.DungeonRunType;
import me.slmnfll.dungeons.instance.DungeonSession;
import me.slmnfll.dungeons.profile.DungeonProfileService;
import me.slmnfll.dungeons.tutorial.TutorialObjectiveService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class DungeonContentService implements Listener {
    private static final double HINT_DISTANCE_SQUARED = 32.0D * 32.0D;

    private final JavaPlugin plugin;
    private final DungeonInstanceManager instanceManager;
    private final DungeonLootService lootService;
    private final DungeonItemDeliveryService itemDeliveryService;
    private final DungeonProfileService profileService;
    private final DungeonRiskService riskService;
    private final TutorialObjectiveService tutorialObjectiveService;
    private BukkitTask displayTask;

    public DungeonContentService(
            JavaPlugin plugin,
            DungeonInstanceManager instanceManager,
            DungeonLootService lootService,
            DungeonItemDeliveryService itemDeliveryService,
            DungeonProfileService profileService,
            DungeonRiskService riskService,
            TutorialObjectiveService tutorialObjectiveService
    ) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.instanceManager = Objects.requireNonNull(instanceManager, "instanceManager");
        this.lootService = Objects.requireNonNull(lootService, "lootService");
        this.itemDeliveryService = Objects.requireNonNull(itemDeliveryService, "itemDeliveryService");
        this.profileService = Objects.requireNonNull(profileService, "profileService");
        this.riskService = Objects.requireNonNull(riskService, "riskService");
        this.tutorialObjectiveService = Objects.requireNonNull(
                tutorialObjectiveService,
                "tutorialObjectiveService"
        );
    }

    public void start() {
        if (displayTask == null) {
            displayTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tickDisplays, 10L, 10L);
        }
    }

    public void stop() {
        if (displayTask != null) {
            displayTask.cancel();
            displayTask = null;
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        DungeonSession session = activeSession(event.getPlayer());
        if (session == null) {
            return;
        }

        BlockPosition position = BlockPosition.from(event.getBlock());
        if (session.content().potAt(position).isPresent()) {
            event.setCancelled(true);
            breakPot(event.getPlayer(), session, event.getBlock(), position);
            return;
        }
        if (session.content().chestAt(position).isPresent()) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(Component.text("This treasure chest cannot be broken.", NamedTextColor.RED));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        boolean blockClick = event.getAction() == Action.RIGHT_CLICK_BLOCK
                || event.getAction() == Action.LEFT_CLICK_BLOCK;
        if (!blockClick
                || event.getHand() != EquipmentSlot.HAND
                || event.getClickedBlock() == null) {
            return;
        }

        DungeonSession session = activeSession(event.getPlayer());
        if (session == null) {
            return;
        }

        Block block = event.getClickedBlock();
        BlockPosition position = BlockPosition.from(block);
        if (session.content().potAt(position).isPresent()) {
            event.setCancelled(true);
            breakPot(event.getPlayer(), session, block, position);
            return;
        }

        LockedChestDefinition chest = session.content().chestAt(position).orElse(null);
        if (chest != null
                && event.getAction() == Action.RIGHT_CLICK_BLOCK
                && !unlockChest(event.getPlayer(), session, block, chest)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onProjectileHit(ProjectileHitEvent event) {
        Block hitBlock = event.getHitBlock();
        if (hitBlock == null || !(event.getEntity().getShooter() instanceof Player player)) {
            return;
        }

        DungeonSession session = activeSession(player);
        if (session == null || !hitBlock.getWorld().getUID().equals(session.world().getUID())) {
            return;
        }

        BlockPosition position = BlockPosition.from(hitBlock);
        if (session.content().potAt(position).isEmpty()) {
            return;
        }

        event.setCancelled(true);
        if (breakPot(player, session, hitBlock, position)) {
            event.getEntity().remove();
        }
    }

    private DungeonSession activeSession(Player player) {
        if (!instanceManager.isInDungeon(player)) {
            return null;
        }
        return instanceManager.sessionFor(player).orElse(null);
    }

    private boolean breakPot(Player player, DungeonSession session, Block block, BlockPosition position) {
        if (!tutorialObjectiveService.canBreakPot(player, session)) {
            player.sendMessage(Component.text(
                    "The training seal holds. Follow the current tutorial objective first.",
                    NamedTextColor.RED
            ));
            block.getWorld().playSound(block.getLocation(), Sound.BLOCK_CHEST_LOCKED, 0.7F, 1.3F);
            return false;
        }
        PotDefinition pot = session.content().takePot(position).orElse(null);
        if (pot == null) {
            return false;
        }

        block.setType(Material.AIR, false);
        World world = block.getWorld();
        Location center = position.center(world);
        world.playSound(center, Sound.BLOCK_DECORATED_POT_SHATTER, 1.0F, 1.0F);
        world.spawnParticle(Particle.POOF, center, 8, 0.25D, 0.25D, 0.25D, 0.02D);

        session.progress().recordBrokenPot();
        if (pot.containsKey()) {
            session.progress().addKey();
            player.sendMessage(Component.text("You found a Dungeon Key!", NamedTextColor.GOLD));
            world.playSound(center, Sound.BLOCK_TRIPWIRE_CLICK_ON, 0.8F, 1.6F);
        }

        List<ItemStack> potLoot = session.instance().runType() == DungeonRunType.TUTORIAL
                ? lootService.tutorialPotLoot(pot)
                : lootService.rollPotLoot(
                        pot,
                        session.instance().affinity(),
                        profileService.effectiveLevel(player)
                );
        itemDeliveryService.give(player, riskService.markAtRisk(potLoot, session.instance().id()));
        if (pot.trapped()) {
            world.spawnParticle(Particle.LARGE_SMOKE, center, 12, 0.35D, 0.45D, 0.35D, 0.02D);
            world.playSound(center, Sound.AMBIENT_CAVE, 0.7F, 1.7F);
            player.sendMessage(Component.text("The pot concealed a trap!", NamedTextColor.RED));
            player.damage(2.0D);
        }
        showProgress(player, session);
        return true;
    }

    private boolean unlockChest(
            Player player,
            DungeonSession session,
            Block block,
            LockedChestDefinition chestDefinition
    ) {
        if (chestDefinition.unlocked()) {
            return true;
        }
        if (!session.progress().consumeKey()) {
            player.sendMessage(Component.text("This chest requires one Dungeon Key.", NamedTextColor.RED));
            block.getWorld().playSound(block.getLocation(), Sound.BLOCK_CHEST_LOCKED, 0.8F, 1.0F);
            showProgress(player, session);
            return false;
        }
        if (!(block.getState() instanceof Chest chest)) {
            session.progress().addKey();
            throw new IllegalStateException("A registered dungeon chest is no longer a chest.");
        }

        chestDefinition.unlock();
        session.progress().recordOpenedChest();
        chest.customName(Component.text("Treasure Chest", NamedTextColor.GOLD));
        chest.getBlockInventory().clear();
        List<ItemStack> chestLoot = session.instance().runType() == DungeonRunType.TUTORIAL
                ? lootService.tutorialChestLoot(chestDefinition, session.instance().affinity())
                : lootService.rollChestLoot(
                        chestDefinition,
                        session.instance().affinity(),
                        profileService.effectiveLevel(player)
                );
        for (ItemStack item : riskService.markAtRisk(chestLoot, session.instance().id())) {
            chest.getBlockInventory().addItem(item);
        }
        chest.update(true, false);
        profileService.award(player, 35L);

        player.sendMessage(Component.text("The Dungeon Key unlocks the treasure chest.", NamedTextColor.GOLD));
        block.getWorld().playSound(block.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.8F, 1.15F);
        showProgress(player, session);
        return true;
    }

    private void tickDisplays() {
        for (DungeonSession session : instanceManager.activeSessions()) {
            Player player = Bukkit.getPlayer(session.instance().ownerId());
            if (player == null || !player.isOnline() || !instanceManager.isInDungeon(player)) {
                continue;
            }
            showProgress(player, session);
            showRarityHints(player, session);
        }
    }

    private void showRarityHints(Player player, DungeonSession session) {
        for (Map.Entry<BlockPosition, PotDefinition> entry : session.content().remainingPots().entrySet()) {
            ItemRarity rarity = entry.getValue().boostedRarity();
            if (rarity == null) {
                continue;
            }
            Location location = entry.getKey().center(session.world());
            if (player.getLocation().distanceSquared(location) > HINT_DISTANCE_SQUARED) {
                continue;
            }
            player.spawnParticle(
                    Particle.DUST,
                    location.add(0.0D, 0.55D, 0.0D),
                    12,
                    0.34D,
                    0.48D,
                    0.34D,
                    0.0D,
                    new Particle.DustOptions(rarity.particleColor(), 1.35F)
            );
        }
    }

    private void showProgress(Player player, DungeonSession session) {
        DungeonRunProgress progress = session.progress();
        Component hud = Component.text("Keys: ", NamedTextColor.GRAY)
                .append(Component.text(progress.keys(), NamedTextColor.GOLD))
                .append(Component.text("  •  Pots: ", NamedTextColor.DARK_GRAY))
                .append(Component.text(progress.brokenPots() + "/" + progress.totalPots(), NamedTextColor.GRAY))
                .append(Component.text("  •  Chests: ", NamedTextColor.DARK_GRAY))
                .append(Component.text(progress.openedChests() + "/" + progress.totalChests(), NamedTextColor.GRAY));

        var activeEncounter = session.encounters().activeRoom();
        if (activeEncounter.isPresent()) {
            var encounter = activeEncounter.orElseThrow();
            hud = hud.append(Component.text("  •  Wave: ", NamedTextColor.DARK_GRAY))
                    .append(Component.text(
                            encounter.currentWave() + "/" + encounter.definition().waveCount(),
                            NamedTextColor.RED
                    ))
                    .append(Component.text("  •  Enemies: ", NamedTextColor.DARK_GRAY))
                    .append(Component.text(encounter.livingMobCount(), NamedTextColor.RED));
        } else if (session.encounters().exitDeadlineMillis() > 0L) {
            long remainingSeconds = Math.max(
                    0L,
                    (session.encounters().exitDeadlineMillis() - System.currentTimeMillis() + 999L) / 1_000L
            );
            hud = hud.append(Component.text("  •  Exit: ", NamedTextColor.DARK_GRAY))
                    .append(Component.text(
                            "%d:%02d".formatted(remainingSeconds / 60L, remainingSeconds % 60L),
                            NamedTextColor.AQUA
                    ));
        }
        player.sendActionBar(hud);
    }
}
