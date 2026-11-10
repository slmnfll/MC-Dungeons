package me.slmnfll.dungeons.equipment;

public final class DungeonCarryLimitPolicy {
    public static final int MAX_ARTIFACTS_AND_CONSUMABLES = 10;

    public boolean allows(int carriedAmount) {
        if (carriedAmount < 0) {
            throw new IllegalArgumentException("Carried item amount cannot be negative.");
        }
        return carriedAmount <= MAX_ARTIFACTS_AND_CONSUMABLES;
    }

    public int excess(int carriedAmount) {
        if (carriedAmount < 0) {
            throw new IllegalArgumentException("Carried item amount cannot be negative.");
        }
        return Math.max(0, carriedAmount - MAX_ARTIFACTS_AND_CONSUMABLES);
    }
}
