package me.slmnfll.dungeons.equipment.weapon;

public record DungeonWeaponData(
        DungeonWeaponDefinition definition,
        DungeonWeaponStats stats,
        long rollSeed,
        boolean abilityRevealed
) {
}
