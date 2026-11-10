package me.slmnfll.dungeons.ui;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public final class DungeonGuiItems {
    private DungeonGuiItems() {
    }

    public static ItemStack tooltipFreeGlassPane(Material material) {
        if (material != Material.GLASS_PANE && !material.name().endsWith("_STAINED_GLASS_PANE")) {
            throw new IllegalArgumentException("GUI filler material must be a glass pane.");
        }
        ItemStack pane = new ItemStack(material);
        ItemMeta meta = pane.getItemMeta();
        meta.displayName(null);
        meta.lore(null);
        meta.setHideTooltip(true);
        pane.setItemMeta(meta);
        return pane;
    }
}
