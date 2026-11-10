package me.slmnfll.dungeons.equipment;

public enum DungeonItemCategory {
    ARTIFACT("artifact"),
    CONSUMABLE("consumable"),
    RUNE("rune");

    private final String id;

    DungeonItemCategory(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public boolean countsTowardCarryLimit() {
        return this == ARTIFACT || this == CONSUMABLE;
    }
}
