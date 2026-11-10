package me.slmnfll.dungeons.encounter;

public final class DungeonCombatTuning {
    public static final long EXIT_TIME_MILLIS = 5L * 60L * 1_000L;

    public int enemyLevel(int playerLevel, int depth, boolean boss) {
        return Math.max(1, playerLevel + depth / 2 + (boss ? 3 : 0));
    }

    public int normalMobCount(int playerLevel, int depth, int wave) {
        int levelBonus = Math.min(2, Math.max(0, playerLevel - 1) / 25);
        int depthBonus = Math.min(3, depth / 3);
        int waveBonus = Math.min(1, Math.max(0, wave - 1));
        return 2 + levelBonus + depthBonus + waveBonus;
    }

    public double healthMultiplier(int enemyLevel, boolean boss) {
        double levelScaling = 1.0D + Math.max(0, enemyLevel - 1) * 0.06D;
        return Math.min(50.0D, levelScaling * (boss ? 4.0D : 1.0D));
    }

    public double damageMultiplier(int enemyLevel, boolean boss) {
        double levelScaling = 1.0D + Math.max(0, enemyLevel - 1) * 0.035D;
        return Math.min(20.0D, levelScaling * (boss ? 1.5D : 1.0D));
    }

    public long killExperience(int enemyLevel, boolean boss) {
        return 6L + enemyLevel / 5L + (boss ? 20L : 0L);
    }

    public long roomExperience(EncounterDefinition encounter, int playerLevel) {
        if (encounter.boss()) {
            return 150L + playerLevel * 2L;
        }
        return 20L + encounter.depth() * 3L + (encounter.required() ? 0L : 15L);
    }
}
