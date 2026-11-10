package me.slmnfll.dungeons.instance;

import me.slmnfll.dungeons.content.DungeonRunContent;
import me.slmnfll.dungeons.content.DungeonRunProgress;
import me.slmnfll.dungeons.encounter.DungeonRunEncounters;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.Objects;

public record DungeonSession(
        DungeonInstance instance,
        World world,
        Location returnLocation,
        DungeonPlayerState playerState,
        DungeonRunContent content,
        DungeonRunProgress progress,
        DungeonRunEncounters encounters
) {
    public DungeonSession {
        Objects.requireNonNull(instance, "instance");
        Objects.requireNonNull(world, "world");
        returnLocation = Objects.requireNonNull(returnLocation, "returnLocation").clone();
        Objects.requireNonNull(playerState, "playerState");
        Objects.requireNonNull(content, "content");
        Objects.requireNonNull(progress, "progress");
        Objects.requireNonNull(encounters, "encounters");
    }

    @Override
    public Location returnLocation() {
        return returnLocation.clone();
    }
}
