package me.slmnfll.dungeons.profile;

public final class DungeonProgressionBenefits {
    public static final int LOOT_FIND_CAP_LEVEL = 100;
    private static final double STANDARD_DAMAGE_PER_LEVEL = 0.0025D;
    private static final double POST_CAP_DAMAGE_PER_LEVEL = 0.0005D;
    private static final double LOOT_FIND_PER_LEVEL = 0.0025D;

    public double damageBonus(int level) {
        requireLevel(level);
        int standardLevels = Math.min(LOOT_FIND_CAP_LEVEL - 1, level - 1);
        int postCapLevels = Math.max(0, level - LOOT_FIND_CAP_LEVEL);
        return standardLevels * STANDARD_DAMAGE_PER_LEVEL
                + postCapLevels * POST_CAP_DAMAGE_PER_LEVEL;
    }

    public double specialLootFindBonus(int level) {
        requireLevel(level);
        return Math.min(LOOT_FIND_CAP_LEVEL - 1, level - 1) * LOOT_FIND_PER_LEVEL;
    }

    public double specialLootChanceMultiplier(int level) {
        return 1.0D + specialLootFindBonus(level);
    }

    public NextLevelBenefits nextLevelBenefits(int currentLevel) {
        requireLevel(currentLevel);
        return new NextLevelBenefits(
                currentLevel + 1,
                currentLevel < LOOT_FIND_CAP_LEVEL
                        ? STANDARD_DAMAGE_PER_LEVEL
                        : POST_CAP_DAMAGE_PER_LEVEL,
                currentLevel < LOOT_FIND_CAP_LEVEL ? LOOT_FIND_PER_LEVEL : 0.0D,
                currentLevel >= LOOT_FIND_CAP_LEVEL
        );
    }

    private void requireLevel(int level) {
        if (level < 1) {
            throw new IllegalArgumentException("Dungeon Level must be at least one.");
        }
    }

    public record NextLevelBenefits(
            int level,
            double damageBonus,
            double specialLootFindBonus,
            boolean lootFindCapped
    ) {
    }
}
