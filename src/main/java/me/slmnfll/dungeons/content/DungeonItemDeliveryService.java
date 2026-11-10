package me.slmnfll.dungeons.content;

import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;

public final class DungeonItemDeliveryService {
    public void give(Player player, List<ItemStack> items) {
        Map<Integer, ItemStack> leftovers = player.getInventory().addItem(items.toArray(ItemStack[]::new));
        for (ItemStack leftover : leftovers.values()) {
            Item dropped = player.getWorld().dropItem(player.getLocation().add(0.0D, 0.1D, 0.0D), leftover);
            dropped.setOwner(player.getUniqueId());
        }
    }
}
