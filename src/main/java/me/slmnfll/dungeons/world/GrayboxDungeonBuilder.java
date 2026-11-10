package me.slmnfll.dungeons.world;

import me.slmnfll.dungeons.layout.DungeonLayout;
import me.slmnfll.dungeons.layout.GridPosition;
import me.slmnfll.dungeons.layout.RoomNode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;

public final class GrayboxDungeonBuilder implements DungeonBuilder {
    private static final int ROOM_HEIGHT = 6;
    private static final int CORRIDOR_RADIUS = 1;

    @Override
    public DungeonBuildResult build(World world, DungeonLayout layout) {
        for (RoomNode room : layout.rooms()) {
            buildRoom(world, room);
        }

        for (RoomNode room : layout.rooms()) {
            for (int connectedId : room.connections()) {
                if (room.id() < connectedId) {
                    RoomNode connected = layout.roomById(connectedId).orElseThrow();
                    buildCorridor(world, room.position(), connected.position());
                }
            }
        }

        RoomNode start = layout.roomById(layout.startRoomId()).orElseThrow();
        int startX = worldCoordinate(start.position().x());
        int startZ = worldCoordinate(start.position().z());
        world.setSpawnLocation(startX, DungeonGeometry.FLOOR_Y + 1, startZ);
        Location spawn = new Location(world, startX + 0.5D, DungeonGeometry.FLOOR_Y + 1.0D, startZ + 0.5D, -90.0F, 0.0F);

        int minX = layout.rooms().stream().mapToInt(room -> worldCoordinate(room.position().x())).min().orElse(startX);
        int maxX = layout.rooms().stream().mapToInt(room -> worldCoordinate(room.position().x())).max().orElse(startX);
        int minZ = layout.rooms().stream().mapToInt(room -> worldCoordinate(room.position().z())).min().orElse(startZ);
        int maxZ = layout.rooms().stream().mapToInt(room -> worldCoordinate(room.position().z())).max().orElse(startZ);
        Location mapCenter = new Location(world, (minX + maxX) / 2.0D, DungeonGeometry.FLOOR_Y, (minZ + maxZ) / 2.0D);
        return new DungeonBuildResult(spawn, mapCenter);
    }

    private void buildRoom(World world, RoomNode room) {
        int centerX = worldCoordinate(room.position().x());
        int centerZ = worldCoordinate(room.position().z());

        for (int x = centerX - DungeonGeometry.ROOM_RADIUS; x <= centerX + DungeonGeometry.ROOM_RADIUS; x++) {
            for (int z = centerZ - DungeonGeometry.ROOM_RADIUS; z <= centerZ + DungeonGeometry.ROOM_RADIUS; z++) {
                set(world, x, DungeonGeometry.FLOOR_Y, z, Material.SMOOTH_STONE);
                set(world, x, DungeonGeometry.FLOOR_Y + ROOM_HEIGHT, z, Material.SMOOTH_STONE_SLAB);

                boolean wall = x == centerX - DungeonGeometry.ROOM_RADIUS || x == centerX + DungeonGeometry.ROOM_RADIUS
                        || z == centerZ - DungeonGeometry.ROOM_RADIUS || z == centerZ + DungeonGeometry.ROOM_RADIUS;
                for (int y = DungeonGeometry.FLOOR_Y + 1; y < DungeonGeometry.FLOOR_Y + ROOM_HEIGHT; y++) {
                    set(world, x, y, z, wall ? Material.GRAY_CONCRETE : Material.AIR);
                }
            }
        }

        Material marker = switch (room.type()) {
            case START -> Material.LIME_CONCRETE;
            case COMBAT -> Material.RED_CONCRETE;
            case PUZZLE -> Material.YELLOW_CONCRETE;
            case NPC -> Material.CYAN_CONCRETE;
            case TREASURE -> Material.GOLD_BLOCK;
            case SECRET -> Material.PURPLE_CONCRETE;
            case BOSS -> Material.BLACK_CONCRETE;
        };
        set(world, centerX, DungeonGeometry.FLOOR_Y, centerZ, marker);
    }

    private void buildCorridor(World world, GridPosition from, GridPosition to) {
        int fromX = worldCoordinate(from.x());
        int fromZ = worldCoordinate(from.z());
        int toX = worldCoordinate(to.x());
        int toZ = worldCoordinate(to.z());

        if (fromX != toX) {
            int minX = Math.min(fromX, toX);
            int maxX = Math.max(fromX, toX);
            for (int x = minX; x <= maxX; x++) {
                carveCorridorCell(world, x, fromZ, true);
            }
        } else {
            int minZ = Math.min(fromZ, toZ);
            int maxZ = Math.max(fromZ, toZ);
            for (int z = minZ; z <= maxZ; z++) {
                carveCorridorCell(world, fromX, z, false);
            }
        }
    }

    private void carveCorridorCell(World world, int x, int z, boolean alongX) {
        for (int offset = -CORRIDOR_RADIUS; offset <= CORRIDOR_RADIUS; offset++) {
            int blockX = alongX ? x : x + offset;
            int blockZ = alongX ? z + offset : z;
            set(world, blockX, DungeonGeometry.FLOOR_Y, blockZ, Material.STONE_BRICKS);
            for (int y = DungeonGeometry.FLOOR_Y + 1; y <= DungeonGeometry.FLOOR_Y + 3; y++) {
                set(world, blockX, y, blockZ, Material.AIR);
            }
        }
    }

    private int worldCoordinate(int gridCoordinate) {
        return DungeonGeometry.worldCoordinate(gridCoordinate);
    }

    private void set(World world, int x, int y, int z, Material material) {
        world.getBlockAt(x, y, z).setType(material, false);
    }

}
