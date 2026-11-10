package me.slmnfll.dungeons.equipment.rune;

public enum DungeonRuneTarget {
    WEAPON("Weapons"),
    ARMOR("Armor"),
    EQUIPMENT("Dungeon Equipment");

    private final String displayName;

    DungeonRuneTarget(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

    public boolean supports(boolean weapon) {
        return this == EQUIPMENT || (weapon ? this == WEAPON : this == ARMOR);
    }
}
