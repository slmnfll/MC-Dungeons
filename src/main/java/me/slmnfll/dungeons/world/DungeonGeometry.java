package me.slmnfll.dungeons.world;

public final class DungeonGeometry {
    public static final int FLOOR_Y = 64;
    public static final int ROOM_SPACING = 20;
    public static final int ROOM_RADIUS = 7;

    private DungeonGeometry() {
    }

    public static int worldCoordinate(int gridCoordinate) {
        return gridCoordinate * ROOM_SPACING;
    }
}
