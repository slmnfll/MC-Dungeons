package me.slmnfll.dungeons.equipment;

import org.bukkit.Material;

public final class DungeonEquipmentPolicy {
    private DungeonEquipmentPolicy() {
    }

    public static boolean isRestrictedVanillaEquipment(Material material) {
        String name = material.name();
        return name.endsWith("_SWORD")
                || name.endsWith("_AXE")
                || name.endsWith("_PICKAXE")
                || name.endsWith("_SHOVEL")
                || name.endsWith("_HOE")
                || name.endsWith("_HELMET")
                || name.endsWith("_CHESTPLATE")
                || name.endsWith("_LEGGINGS")
                || name.endsWith("_BOOTS")
                || material == Material.BOW
                || material == Material.CROSSBOW
                || material == Material.TRIDENT
                || material == Material.MACE
                || material == Material.SHIELD
                || material == Material.ELYTRA;
    }
}
