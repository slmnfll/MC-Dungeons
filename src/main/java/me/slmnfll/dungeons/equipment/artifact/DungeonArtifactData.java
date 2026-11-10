package me.slmnfll.dungeons.equipment.artifact;

import java.util.Objects;

public record DungeonArtifactData(
        DungeonArtifactDefinition definition,
        boolean abilityRevealed
) {
    public DungeonArtifactData {
        Objects.requireNonNull(definition, "definition");
    }
}
