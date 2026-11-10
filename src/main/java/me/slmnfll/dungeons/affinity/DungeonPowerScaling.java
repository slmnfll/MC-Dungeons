package me.slmnfll.dungeons.affinity;

public final class DungeonPowerScaling {
    private static final int NORMAL_GROWTH_LEVELS = 99;
    private static final double NORMAL_DAMAGE_PER_LEVEL = 0.0025D;
    private static final double POST_100_DAMAGE_PER_LEVEL = 0.0005D;

    public double levelDamageMultiplier(int dungeonLevel) {
        if (dungeonLevel < 1) {
            throw new IllegalArgumentException("Dungeon Level must be at least 1.");
        }
        int earnedLevels = dungeonLevel - 1;
        int normalGrowth = Math.min(NORMAL_GROWTH_LEVELS, earnedLevels);
        int post100Growth = Math.max(0, earnedLevels - NORMAL_GROWTH_LEVELS);
        return 1.0D
                + normalGrowth * NORMAL_DAMAGE_PER_LEVEL
                + post100Growth * POST_100_DAMAGE_PER_LEVEL;
    }

    public double weaponDamageMultiplier(
            int dungeonLevel,
            Affinity selectedAffinity,
            Affinity weaponAffinity
    ) {
        double multiplier = levelDamageMultiplier(dungeonLevel);
        if (selectedAffinity == weaponAffinity) {
            multiplier *= 1.0D + Affinity.EFFECTIVENESS_BONUS;
        }
        return multiplier;
    }
}
