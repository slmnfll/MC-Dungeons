package me.slmnfll.dungeons.content;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;

public record BlockPosition(int x, int y, int z) {
    public static BlockPosition from(Block block) {
        return new BlockPosition(block.getX(), block.getY(), block.getZ());
    }

    public Location center(World world) {
        return new Location(world, x + 0.5D, y + 0.7D, z + 0.5D);
    }
}
