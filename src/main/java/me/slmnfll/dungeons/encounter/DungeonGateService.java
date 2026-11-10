package me.slmnfll.dungeons.encounter;

import me.slmnfll.dungeons.content.BlockPosition;
import me.slmnfll.dungeons.layout.DungeonLayout;
import me.slmnfll.dungeons.layout.RoomNode;
import me.slmnfll.dungeons.world.DungeonGeometry;
import org.bukkit.Material;
import org.bukkit.World;

import java.util.HashSet;
import java.util.Set;

public final class DungeonGateService {
    public void seal(World world, DungeonLayout layout, int roomId, Material gateMaterial) {
        for (BlockPosition position : gatePositions(layout, roomId)) {
            world.getBlockAt(position.x(), position.y(), position.z()).setType(gateMaterial, false);
        }
    }

    public void open(World world, DungeonLayout layout, int roomId) {
        for (BlockPosition position : gatePositions(layout, roomId)) {
            world.getBlockAt(position.x(), position.y(), position.z()).setType(Material.AIR, false);
        }
    }

    public Set<BlockPosition> gatePositions(DungeonLayout layout, int roomId) {
        RoomNode room = layout.roomById(roomId).orElseThrow();
        int centerX = DungeonGeometry.worldCoordinate(room.position().x());
        int centerZ = DungeonGeometry.worldCoordinate(room.position().z());
        Set<BlockPosition> positions = new HashSet<>();

        for (int connectedId : room.connections()) {
            RoomNode connected = layout.roomById(connectedId).orElseThrow();
            int deltaX = Integer.signum(connected.position().x() - room.position().x());
            int deltaZ = Integer.signum(connected.position().z() - room.position().z());
            int gateX = centerX + deltaX * DungeonGeometry.ROOM_RADIUS;
            int gateZ = centerZ + deltaZ * DungeonGeometry.ROOM_RADIUS;

            for (int width = -1; width <= 1; width++) {
                for (int y = DungeonGeometry.FLOOR_Y + 1; y <= DungeonGeometry.FLOOR_Y + 3; y++) {
                    positions.add(new BlockPosition(
                            gateX + (deltaZ != 0 ? width : 0),
                            y,
                            gateZ + (deltaX != 0 ? width : 0)
                    ));
                }
            }
        }
        return positions;
    }
}
