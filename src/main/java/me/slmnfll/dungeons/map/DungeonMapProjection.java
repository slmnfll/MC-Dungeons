package me.slmnfll.dungeons.map;

import me.slmnfll.dungeons.layout.DungeonLayout;
import me.slmnfll.dungeons.layout.RoomNode;
import me.slmnfll.dungeons.world.DungeonGeometry;

import java.util.Objects;

final class DungeonMapProjection {
    static final int MAP_SIZE = 128;
    static final int PADDING = 8;

    private final double scale;
    private final double offsetX;
    private final double offsetZ;

    DungeonMapProjection(DungeonLayout layout) {
        Objects.requireNonNull(layout, "layout");
        if (layout.rooms().isEmpty()) {
            throw new IllegalArgumentException("A dungeon map needs at least one room.");
        }

        double minX = layout.rooms().stream()
                .mapToDouble(room -> roomCenterX(room) - DungeonGeometry.ROOM_RADIUS)
                .min()
                .orElseThrow();
        double maxX = layout.rooms().stream()
                .mapToDouble(room -> roomCenterX(room) + DungeonGeometry.ROOM_RADIUS)
                .max()
                .orElseThrow();
        double minZ = layout.rooms().stream()
                .mapToDouble(room -> roomCenterZ(room) - DungeonGeometry.ROOM_RADIUS)
                .min()
                .orElseThrow();
        double maxZ = layout.rooms().stream()
                .mapToDouble(room -> roomCenterZ(room) + DungeonGeometry.ROOM_RADIUS)
                .max()
                .orElseThrow();

        double drawableSize = MAP_SIZE - (PADDING * 2.0D) - 1.0D;
        double width = Math.max(1.0D, maxX - minX);
        double depth = Math.max(1.0D, maxZ - minZ);
        this.scale = Math.min(drawableSize / width, drawableSize / depth);

        double renderedWidth = width * scale;
        double renderedDepth = depth * scale;
        this.offsetX = PADDING + ((drawableSize - renderedWidth) / 2.0D) - (minX * scale);
        this.offsetZ = PADDING + ((drawableSize - renderedDepth) / 2.0D) - (minZ * scale);
    }

    int roomCenterPixelX(RoomNode room) {
        return pixelX(roomCenterX(room));
    }

    int roomCenterPixelZ(RoomNode room) {
        return pixelZ(roomCenterZ(room));
    }

    int roomRadiusPixels() {
        return Math.max(3, (int) Math.round(DungeonGeometry.ROOM_RADIUS * scale));
    }

    int pixelX(double worldX) {
        return clampPixel((int) Math.round((worldX * scale) + offsetX));
    }

    int pixelZ(double worldZ) {
        return clampPixel((int) Math.round((worldZ * scale) + offsetZ));
    }

    byte cursorX(double worldX) {
        return cursorCoordinate(pixelX(worldX));
    }

    byte cursorZ(double worldZ) {
        return cursorCoordinate(pixelZ(worldZ));
    }

    boolean contains(double worldX, double worldZ) {
        double pixelX = (worldX * scale) + offsetX;
        double pixelZ = (worldZ * scale) + offsetZ;
        return pixelX >= 0.0D && pixelX < MAP_SIZE && pixelZ >= 0.0D && pixelZ < MAP_SIZE;
    }

    private static double roomCenterX(RoomNode room) {
        return DungeonGeometry.worldCoordinate(room.position().x());
    }

    private static double roomCenterZ(RoomNode room) {
        return DungeonGeometry.worldCoordinate(room.position().z());
    }

    private static int clampPixel(int pixel) {
        return Math.max(0, Math.min(MAP_SIZE - 1, pixel));
    }

    private static byte cursorCoordinate(int pixel) {
        return (byte) Math.max(-128, Math.min(127, (pixel - 64) * 2));
    }
}
