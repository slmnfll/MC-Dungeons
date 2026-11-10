package me.slmnfll.dungeons.content;

public final class LockedChestDefinition {
    private final int roomId;
    private final long lootSeed;
    private boolean unlocked;

    public LockedChestDefinition(int roomId, long lootSeed) {
        this.roomId = roomId;
        this.lootSeed = lootSeed;
    }

    public int roomId() {
        return roomId;
    }

    public long lootSeed() {
        return lootSeed;
    }

    public boolean unlocked() {
        return unlocked;
    }

    public void unlock() {
        if (unlocked) {
            throw new IllegalStateException("The chest is already unlocked.");
        }
        unlocked = true;
    }
}
