package me.slmnfll.dungeons.equipment.armor;

import java.util.Objects;

public record DungeonArmorData(
        DungeonArmorDefinition definition,
        DungeonArmorStats stats,
        long rollSeed
) {
    public DungeonArmorData {
        Objects.requireNonNull(definition, "definition");
        Objects.requireNonNull(stats, "stats");
    }
}
