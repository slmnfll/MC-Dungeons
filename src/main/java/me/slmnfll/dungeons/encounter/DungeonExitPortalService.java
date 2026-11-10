package me.slmnfll.dungeons.encounter;

import me.slmnfll.dungeons.content.BlockPosition;
import me.slmnfll.dungeons.layout.DungeonLayout;
import me.slmnfll.dungeons.layout.RoomNode;
import me.slmnfll.dungeons.world.DungeonGeometry;
import org.bukkit.Axis;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Orientable;

import java.util.HashSet;
import java.util.Set;

public final class DungeonExitPortalService {
    public Set<BlockPosition> create(World world, DungeonLayout layout) {
        RoomNode bossRoom = layout.roomById(layout.bossRoomId()).orElseThrow();
        int portalX = DungeonGeometry.worldCoordinate(bossRoom.position().x()) + 4;
        int centerZ = DungeonGeometry.worldCoordinate(bossRoom.position().z());

        for (int z = centerZ - 2; z <= centerZ + 2; z++) {
            set(world, portalX, DungeonGeometry.FLOOR_Y, z, Material.CRYING_OBSIDIAN);
            set(world, portalX, DungeonGeometry.FLOOR_Y + 4, z, Material.CRYING_OBSIDIAN);
        }
        for (int y = DungeonGeometry.FLOOR_Y + 1; y <= DungeonGeometry.FLOOR_Y + 3; y++) {
            set(world, portalX, y, centerZ - 2, Material.CRYING_OBSIDIAN);
            set(world, portalX, y, centerZ + 2, Material.CRYING_OBSIDIAN);
        }

        BlockData portalData = Material.NETHER_PORTAL.createBlockData();
        if (portalData instanceof Orientable orientable) {
            orientable.setAxis(Axis.Z);
        }
        Set<BlockPosition> portalBlocks = new HashSet<>();
        for (int z = centerZ - 1; z <= centerZ + 1; z++) {
            for (int y = DungeonGeometry.FLOOR_Y + 1; y <= DungeonGeometry.FLOOR_Y + 3; y++) {
                world.getBlockAt(portalX, y, z).setBlockData(portalData, false);
                portalBlocks.add(new BlockPosition(portalX, y, z));
            }
        }
        return portalBlocks;
    }

    private void set(World world, int x, int y, int z, Material material) {
        world.getBlockAt(x, y, z).setType(material, false);
    }
}
