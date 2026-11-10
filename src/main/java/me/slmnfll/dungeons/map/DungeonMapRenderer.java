package me.slmnfll.dungeons.map;

import me.slmnfll.dungeons.layout.DungeonLayout;
import me.slmnfll.dungeons.layout.RoomNode;
import me.slmnfll.dungeons.layout.RoomType;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapCursor;
import org.bukkit.map.MapCursorCollection;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;

import java.awt.Color;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class DungeonMapRenderer extends MapRenderer {
    private static final Color BACKGROUND = new Color(25, 25, 29);
    private static final Color CORRIDOR = new Color(116, 116, 116);
    private static final Color ROOM_BORDER = new Color(214, 214, 206);
    private static final Color START = new Color(90, 170, 65);
    private static final Color COMBAT = new Color(155, 61, 54);
    private static final Color PUZZLE = new Color(194, 166, 58);
    private static final Color NPC = new Color(52, 153, 167);
    private static final Color TREASURE = new Color(224, 177, 44);
    private static final Color SECRET = new Color(119, 65, 145);
    private static final Color BOSS = new Color(88, 25, 28);

    private final DungeonLayout layout;
    private final UUID dungeonWorldId;
    private final DungeonMapProjection projection;
    private final Set<UUID> paintedFor = ConcurrentHashMap.newKeySet();

    public DungeonMapRenderer(DungeonLayout layout, UUID dungeonWorldId) {
        super(true);
        this.layout = Objects.requireNonNull(layout, "layout");
        this.dungeonWorldId = Objects.requireNonNull(dungeonWorldId, "dungeonWorldId");
        this.projection = new DungeonMapProjection(layout);
    }

    @Override
    public void render(MapView map, MapCanvas canvas, Player player) {
        if (paintedFor.add(player.getUniqueId())) {
            paintBlueprint(canvas);
        }

        MapCursorCollection cursors = new MapCursorCollection();
        Location location = player.getLocation();
        if (player.getWorld().getUID().equals(dungeonWorldId)
                && projection.contains(location.getX(), location.getZ())) {
            cursors.addCursor(new MapCursor(
                    projection.cursorX(location.getX()),
                    projection.cursorZ(location.getZ()),
                    cursorDirection(location.getYaw()),
                    MapCursor.Type.PLAYER,
                    true
            ));
        }
        canvas.setCursors(cursors);
    }

    static byte cursorDirection(float yaw) {
        return (byte) Math.floorMod(Math.round(yaw / 22.5F), 16);
    }

    private void paintBlueprint(MapCanvas canvas) {
        fill(canvas, BACKGROUND);

        for (RoomNode room : layout.rooms()) {
            for (int connectedId : room.connections()) {
                if (room.id() >= connectedId) {
                    continue;
                }
                RoomNode connected = layout.roomById(connectedId).orElseThrow();
                drawLine(
                        canvas,
                        projection.roomCenterPixelX(room),
                        projection.roomCenterPixelZ(room),
                        projection.roomCenterPixelX(connected),
                        projection.roomCenterPixelZ(connected),
                        2,
                        CORRIDOR
                );
            }
        }

        int radius = projection.roomRadiusPixels();
        for (RoomNode room : layout.rooms()) {
            int centerX = projection.roomCenterPixelX(room);
            int centerZ = projection.roomCenterPixelZ(room);
            drawRectangle(canvas, centerX - radius, centerZ - radius, centerX + radius, centerZ + radius, ROOM_BORDER);
            drawRectangle(
                    canvas,
                    centerX - radius + 2,
                    centerZ - radius + 2,
                    centerX + radius - 2,
                    centerZ + radius - 2,
                    colorFor(room.type())
            );
        }
    }

    private static Color colorFor(RoomType type) {
        return switch (type) {
            case START -> START;
            case COMBAT -> COMBAT;
            case PUZZLE -> PUZZLE;
            case NPC -> NPC;
            case TREASURE -> TREASURE;
            case SECRET -> SECRET;
            case BOSS -> BOSS;
        };
    }

    private static void fill(MapCanvas canvas, Color color) {
        for (int x = 0; x < DungeonMapProjection.MAP_SIZE; x++) {
            for (int z = 0; z < DungeonMapProjection.MAP_SIZE; z++) {
                canvas.setPixelColor(x, z, color);
            }
        }
    }

    private static void drawRectangle(MapCanvas canvas, int minX, int minZ, int maxX, int maxZ, Color color) {
        int safeMinX = Math.max(0, minX);
        int safeMinZ = Math.max(0, minZ);
        int safeMaxX = Math.min(DungeonMapProjection.MAP_SIZE - 1, maxX);
        int safeMaxZ = Math.min(DungeonMapProjection.MAP_SIZE - 1, maxZ);
        for (int x = safeMinX; x <= safeMaxX; x++) {
            for (int z = safeMinZ; z <= safeMaxZ; z++) {
                canvas.setPixelColor(x, z, color);
            }
        }
    }

    private static void drawLine(
            MapCanvas canvas,
            int fromX,
            int fromZ,
            int toX,
            int toZ,
            int radius,
            Color color
    ) {
        int deltaX = Math.abs(toX - fromX);
        int stepX = fromX < toX ? 1 : -1;
        int deltaZ = -Math.abs(toZ - fromZ);
        int stepZ = fromZ < toZ ? 1 : -1;
        int error = deltaX + deltaZ;
        int x = fromX;
        int z = fromZ;

        while (true) {
            drawRectangle(canvas, x - radius, z - radius, x + radius, z + radius, color);
            if (x == toX && z == toZ) {
                return;
            }
            int doubledError = error * 2;
            if (doubledError >= deltaZ) {
                error += deltaZ;
                x += stepX;
            }
            if (doubledError <= deltaX) {
                error += deltaX;
                z += stepZ;
            }
        }
    }
}
