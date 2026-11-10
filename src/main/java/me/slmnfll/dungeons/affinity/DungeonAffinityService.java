package me.slmnfll.dungeons.affinity;

import me.slmnfll.dungeons.instance.DungeonInstanceManager;
import me.slmnfll.dungeons.profile.DungeonProfileService;
import org.bukkit.entity.Player;

import java.util.Objects;

public final class DungeonAffinityService {
    private final DungeonProfileService profileService;
    private final DungeonInstanceManager instanceManager;

    public DungeonAffinityService(
            DungeonProfileService profileService,
            DungeonInstanceManager instanceManager
    ) {
        this.profileService = Objects.requireNonNull(profileService, "profileService");
        this.instanceManager = Objects.requireNonNull(instanceManager, "instanceManager");
    }

    public SelectionResult select(Player player, Affinity affinity) {
        if (instanceManager.sessionFor(player).isPresent()) {
            throw new IllegalStateException("You cannot change affinity during an active dungeon run.");
        }
        boolean changed = profileService.selectAffinity(player, affinity);
        return new SelectionResult(affinity, changed);
    }

    public record SelectionResult(Affinity affinity, boolean changed) {
    }
}
