package me.slmnfll.dungeons.world;

import org.bukkit.Location;

public record DungeonBuildResult(Location spawn, Location mapCenter) {
    public DungeonBuildResult {
        spawn = spawn.clone();
        mapCenter = mapCenter.clone();
    }

    @Override
    public Location spawn() {
        return spawn.clone();
    }

    @Override
    public Location mapCenter() {
        return mapCenter.clone();
    }
}
