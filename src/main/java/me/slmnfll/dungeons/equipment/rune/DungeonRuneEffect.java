package me.slmnfll.dungeons.equipment.rune;

public enum DungeonRuneEffect {
    FORCE("Force", true),
    WARD("Ward", true),
    CINDERS("Cinder Burst", true),
    RIFT("Rift Pull", true),
    RESTORATION("Restoration", false);

    private final String displayName;
    private final boolean socketable;

    DungeonRuneEffect(String displayName, boolean socketable) {
        this.displayName = displayName;
        this.socketable = socketable;
    }

    public String displayName() {
        return displayName;
    }

    public boolean socketable() {
        return socketable;
    }
}
