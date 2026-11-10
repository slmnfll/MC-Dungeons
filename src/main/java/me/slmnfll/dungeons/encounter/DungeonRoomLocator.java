package me.slmnfll.dungeons.encounter;

import me.slmnfll.dungeons.layout.DungeonLayout;
import me.slmnfll.dungeons.layout.RoomNode;
import me.slmnfll.dungeons.world.DungeonGeometry;
import org.bukkit.Location;

import java.util.Optional;

public final class DungeonRoomLocator {
    public Optional<RoomNode> locate(DungeonLayout layout, Location location) {
        return layout.rooms().stream().filter(room -> {
            int centerX = DungeonGeometry.worldCoordinate(room.position().x());
            int centerZ = DungeonGeometry.worldCoordinate(room.position().z());
            return Math.abs(location.getX() - centerX) <= DungeonGeometry.ROOM_RADIUS - 1
                    && Math.abs(location.getZ() - centerZ) <= DungeonGeometry.ROOM_RADIUS - 1;
        }).findFirst();
    }
}
