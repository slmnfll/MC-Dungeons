package me.slmnfll.dungeons.content;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class DungeonRunContent {
    private final Map<BlockPosition, PotDefinition> remainingPots;
    private final Map<BlockPosition, LockedChestDefinition> chests;
    private final int totalPots;

    public DungeonRunContent(DungeonContentPlan plan) {
        this.remainingPots = new LinkedHashMap<>(plan.pots());
        this.chests = new LinkedHashMap<>(plan.chests());
        this.totalPots = plan.pots().size();
    }

    public Optional<PotDefinition> takePot(BlockPosition position) {
        return Optional.ofNullable(remainingPots.remove(position));
    }

    public Optional<PotDefinition> potAt(BlockPosition position) {
        return Optional.ofNullable(remainingPots.get(position));
    }

    public Optional<LockedChestDefinition> chestAt(BlockPosition position) {
        return Optional.ofNullable(chests.get(position));
    }

    public Map<BlockPosition, PotDefinition> remainingPots() {
        return Map.copyOf(remainingPots);
    }

    public Collection<LockedChestDefinition> chests() {
        return java.util.List.copyOf(chests.values());
    }

    public int totalPots() {
        return totalPots;
    }

    public int totalChests() {
        return chests.size();
    }
}
