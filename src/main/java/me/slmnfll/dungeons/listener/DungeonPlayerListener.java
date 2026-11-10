package me.slmnfll.dungeons.listener;

import me.slmnfll.dungeons.content.DungeonDeathLootResult;
import me.slmnfll.dungeons.instance.DungeonInstanceManager;
import me.slmnfll.dungeons.instance.DungeonRunType;
import me.slmnfll.dungeons.map.ExpeditionMapService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class DungeonPlayerListener implements Listener {
    private final JavaPlugin plugin;
    private final DungeonInstanceManager instanceManager;
    private final ExpeditionMapService mapService;

    public DungeonPlayerListener(
            JavaPlugin plugin,
            DungeonInstanceManager instanceManager,
            ExpeditionMapService mapService
    ) {
        this.plugin = plugin;
        this.instanceManager = instanceManager;
        this.mapService = mapService;
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (instanceManager.sessionFor(event.getBlock().getWorld()).isPresent()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (instanceManager.sessionFor(event.getBlock().getWorld()).isPresent()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onMapDrop(PlayerDropItemEvent event) {
        if (mapService.isTemporaryMap(event.getItemDrop().getItemStack())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(Component.text("The expedition map cannot leave your inventory.", NamedTextColor.RED));
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (mapService.isTemporaryMap(event.getCurrentItem()) || mapService.isTemporaryMap(event.getCursor())) {
            event.setCancelled(true);
            return;
        }
        if (event.getWhoClicked() instanceof Player player
                && event.getHotbarButton() == ExpeditionMapService.HOTBAR_SLOT
                && mapService.isTemporaryMap(player.getInventory().getItem(ExpeditionMapService.HOTBAR_SLOT))) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (mapService.isTemporaryMap(event.getOldCursor())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onSwapHands(PlayerSwapHandItemsEvent event) {
        if (mapService.isTemporaryMap(event.getMainHandItem()) || mapService.isTemporaryMap(event.getOffHandItem())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPortal(PlayerPortalEvent event) {
        if (instanceManager.isInDungeon(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        if (event.getEntity() instanceof Player player && instanceManager.isInDungeon(player)) {
            event.setCancelled(true);
            player.setFoodLevel(20);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onNaturalRegeneration(EntityRegainHealthEvent event) {
        if (event.getEntity() instanceof Player player
                && instanceManager.isInDungeon(player)
                && event.getRegainReason() == EntityRegainHealthEvent.RegainReason.SATIATED) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldChange(PlayerChangedWorldEvent event) {
        if (instanceManager.sessionFor(event.getFrom()).isPresent()) {
            Bukkit.getScheduler().runTask(plugin, () -> instanceManager.handleExternalExit(event.getPlayer()));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getPlayer();
        boolean tutorial = instanceManager.sessionFor(player)
                .filter(session -> session.instance().runType() == DungeonRunType.TUTORIAL)
                .isPresent();
        DungeonDeathLootResult lootResult = instanceManager.markFailedByDeath(player).orElse(null);
        if (lootResult == null) {
            return;
        }

        event.setKeepInventory(true);
        event.getDrops().clear();
        event.setKeepLevel(true);
        event.setDroppedExp(0);
        lootResult.droppedStacks().forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));
        if (tutorial) {
            player.sendMessage(Component.text(
                    "The Guild Chronicler has recorded your tactical inspection of the floor. "
                            + "Your starter gear is safe; " + lootResult.lostAmount()
                            + " unsecured item(s) were not.",
                    NamedTextColor.YELLOW
            ));
        } else {
            player.sendMessage(Component.text(
                    "The expedition has ended. " + lootResult.keptAmount() + " unsecured item(s) survived; "
                            + lootResult.lostAmount() + " were left behind.",
                    NamedTextColor.RED
            ));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onRespawn(PlayerRespawnEvent event) {
        instanceManager.failedRunRespawnLocation(event.getPlayer()).ifPresent(location -> {
            event.setRespawnLocation(location);
            Bukkit.getScheduler().runTask(plugin, () -> instanceManager.finishFailedRun(event.getPlayer()));
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        instanceManager.handleDisconnect(event.getPlayer());
    }
}
