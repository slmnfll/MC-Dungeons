package me.slmnfll.dungeons.equipment.weapon;

public record DungeonWeaponStats(double damage, double speed, int maxDurability) {
    public DungeonWeaponStats {
        if (damage <= 0.0D || speed <= 0.0D || maxDurability < 0) {
            throw new IllegalArgumentException("Invalid dungeon weapon stats.");
        }
    }
}
