package me.slmnfll.dungeons.equipment.artifact;

import me.slmnfll.dungeons.affinity.Affinity;
import me.slmnfll.dungeons.content.ItemRarity;
import org.bukkit.Material;

import java.util.Objects;

public record DungeonArtifactDefinition(
        String id,
        String displayName,
        Affinity affinity,
        Material material,
        ItemRarity rarity,
        ArtifactAbility ability,
        double power,
        double radius,
        int durationTicks,
        int cooldownTicks
) {
    public DungeonArtifactDefinition {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(displayName, "displayName");
        Objects.requireNonNull(affinity, "affinity");
        Objects.requireNonNull(material, "material");
        Objects.requireNonNull(rarity, "rarity");
        Objects.requireNonNull(ability, "ability");
        if (id.isBlank() || displayName.isBlank() || power <= 0.0D || radius < 0.0D
                || durationTicks < 0 || cooldownTicks < 1) {
            throw new IllegalArgumentException("Invalid dungeon artifact definition.");
        }
    }
}
