package me.slmnfll.dungeons.content;

public final class DungeonRunProgress {
    private final int totalPots;
    private final int totalChests;
    private int keys;
    private int brokenPots;
    private int openedChests;

    public DungeonRunProgress(DungeonRunContent content) {
        this.totalPots = content.totalPots();
        this.totalChests = content.totalChests();
    }

    public int keys() {
        return keys;
    }

    public int brokenPots() {
        return brokenPots;
    }

    public int totalPots() {
        return totalPots;
    }

    public int openedChests() {
        return openedChests;
    }

    public int totalChests() {
        return totalChests;
    }

    public void recordBrokenPot() {
        brokenPots++;
    }

    public void addKey() {
        keys++;
    }

    public boolean consumeKey() {
        if (keys == 0) {
            return false;
        }
        keys--;
        return true;
    }

    public void recordOpenedChest() {
        openedChests++;
    }
}
