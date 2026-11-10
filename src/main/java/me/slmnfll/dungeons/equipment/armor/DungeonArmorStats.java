package me.slmnfll.dungeons.equipment.armor;

public record DungeonArmorStats(
        double defense,
        double vitality,
        double power,
        int maxDurability
) {
    public DungeonArmorStats {
        if (defense < 0.0D || vitality < 0.0D || power < 0.0D || maxDurability < 0) {
            throw new IllegalArgumentException("Dungeon armor stats cannot be negative.");
        }
    }
}
