package me.slmnfll.dungeons.profile;

public final class DungeonLevelCurve {
    private static final long BASE_EXPERIENCE = 100L;
    private static final long EXPERIENCE_GROWTH_PER_LEVEL = 25L;

    public long experienceRequiredForNextLevel(int level) {
        if (level < 1) {
            throw new IllegalArgumentException("Dungeon Level must be at least 1.");
        }
        return BASE_EXPERIENCE + EXPERIENCE_GROWTH_PER_LEVEL * (level - 1L);
    }

    public float progress(int level, long experienceIntoLevel) {
        long required = experienceRequiredForNextLevel(level);
        return Math.clamp((float) experienceIntoLevel / (float) required, 0.0F, 0.999999F);
    }
}
