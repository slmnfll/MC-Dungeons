package me.slmnfll.dungeons.world;

import me.slmnfll.dungeons.layout.DungeonLayout;
import org.bukkit.World;

public interface DungeonBuilder {
    DungeonBuildResult build(World world, DungeonLayout layout);
}
