package me.slmnfll.dungeons.map;

import me.slmnfll.dungeons.instance.DungeonInstance;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapView;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public final class ExpeditionMapService {
    public static final int HOTBAR_SLOT = 8;

    private final NamespacedKey temporaryMapKey;

    public ExpeditionMapService(JavaPlugin plugin) {
        this.temporaryMapKey = new NamespacedKey(plugin, "temporary_expedition_map");
    }

    public boolean canGive(Player player) {
        PlayerInventory inventory = player.getInventory();
        ItemStack ninthSlot = inventory.getItem(HOTBAR_SLOT);
        return ninthSlot == null || ninthSlot.getType().isAir() || inventory.firstEmpty() >= 0;
    }

    public void give(Player player, World world, DungeonInstance instance) {
        Objects.requireNonNull(player, "player");
        if (!canGive(player)) {
            throw new IllegalStateException("The player needs one free inventory slot for the expedition map.");
        }

        MapView mapView = Bukkit.createMap(world);
        for (org.bukkit.map.MapRenderer renderer : java.util.List.copyOf(mapView.getRenderers())) {
            mapView.removeRenderer(renderer);
        }
        mapView.addRenderer(new DungeonMapRenderer(instance.layout(), world.getUID()));

        ItemStack map = new ItemStack(Material.FILLED_MAP);
        MapMeta meta = (MapMeta) map.getItemMeta();
        meta.displayName(Component.text("Expedition Map", NamedTextColor.AQUA));
        meta.lore(java.util.List.of(
                Component.text("Temporary — removed when the expedition ends", NamedTextColor.GRAY),
                Component.text("Seed: " + instance.seed(), NamedTextColor.DARK_GRAY)
        ));
        meta.setMapView(mapView);
        meta.getPersistentDataContainer().set(temporaryMapKey, PersistentDataType.BYTE, (byte) 1);
        map.setItemMeta(meta);

        PlayerInventory inventory = player.getInventory();
        ItemStack ninthSlot = inventory.getItem(HOTBAR_SLOT);
        if (ninthSlot != null && !ninthSlot.getType().isAir()) {
            int emptySlot = inventory.firstEmpty();
            inventory.setItem(emptySlot, ninthSlot);
        }
        inventory.setItem(HOTBAR_SLOT, map);
    }

    public void remove(Player player) {
        PlayerInventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            if (isTemporaryMap(inventory.getItem(slot))) {
                inventory.setItem(slot, null);
            }
        }
        if (isTemporaryMap(inventory.getItemInOffHand())) {
            inventory.setItemInOffHand(null);
        }
    }

    public boolean isTemporaryMap(ItemStack itemStack) {
        if (itemStack == null || itemStack.getType().isAir() || !itemStack.hasItemMeta()) {
            return false;
        }
        return itemStack.getItemMeta().getPersistentDataContainer().has(temporaryMapKey, PersistentDataType.BYTE);
    }
}
