package me.slmnfll.dungeons.content;

import java.util.Map;

public record DungeonContentPlan(
        Map<BlockPosition, PotDefinition> pots,
        Map<BlockPosition, LockedChestDefinition> chests
) {
    public DungeonContentPlan {
        pots = Map.copyOf(pots);
        chests = Map.copyOf(chests);
    }
}
