package me.slmnfll.dungeons.equipment.rune.ability;

public final class DungeonRuneAbilityScaling {
    public static final int CINDER_HITS_REQUIRED = 5;
    public static final double CINDER_RADIUS = 3.25D;
    private static final double CINDER_BASE_DAMAGE_MULTIPLIER = 0.35D;
    private static final double CINDER_ADDITIONAL_RUNE_MULTIPLIER = 0.15D;
    private static final int CINDER_BASE_FIRE_TICKS = 60;
    private static final int CINDER_ADDITIONAL_RUNE_FIRE_TICKS = 20;
    public static final int RIFT_COOLDOWN_TICKS = 240;
    private static final double RIFT_BASE_RADIUS = 5.5D;
    private static final double RIFT_ADDITIONAL_RUNE_RADIUS = 1.0D;
    private static final double RIFT_BASE_PULL_STRENGTH = 0.65D;
    private static final double RIFT_ADDITIONAL_RUNE_PULL_STRENGTH = 0.15D;
    private static final int RIFT_BASE_SLOW_TICKS = 60;
    private static final int RIFT_ADDITIONAL_RUNE_SLOW_TICKS = 20;

    private DungeonRuneAbilityScaling() {
    }

    public static double cinderDamageMultiplier(int runeCount) {
        requireSocketCount(runeCount);
        return CINDER_BASE_DAMAGE_MULTIPLIER
                + ((runeCount - 1) * CINDER_ADDITIONAL_RUNE_MULTIPLIER);
    }

    public static int cinderFireTicks(int runeCount) {
        requireSocketCount(runeCount);
        return CINDER_BASE_FIRE_TICKS
                + ((runeCount - 1) * CINDER_ADDITIONAL_RUNE_FIRE_TICKS);
    }

    public static double riftRadius(int runeCount) {
        requireSocketCount(runeCount);
        return RIFT_BASE_RADIUS + ((runeCount - 1) * RIFT_ADDITIONAL_RUNE_RADIUS);
    }

    public static double riftPullStrength(int runeCount) {
        requireSocketCount(runeCount);
        return RIFT_BASE_PULL_STRENGTH
                + ((runeCount - 1) * RIFT_ADDITIONAL_RUNE_PULL_STRENGTH);
    }

    public static int riftSlowTicks(int runeCount) {
        requireSocketCount(runeCount);
        return RIFT_BASE_SLOW_TICKS + ((runeCount - 1) * RIFT_ADDITIONAL_RUNE_SLOW_TICKS);
    }

    private static void requireSocketCount(int runeCount) {
        if (runeCount < 1 || runeCount > 3) {
            throw new IllegalArgumentException("Ability rune count must be between one and three.");
        }
    }
}
