package me.slmnfll.dungeons.equipment.rune;

import me.slmnfll.dungeons.content.ItemRarity;
import org.bukkit.Material;

import java.util.Objects;

public record DungeonRuneDefinition(
        String id,
        String displayName,
        Material material,
        ItemRarity rarity,
        DungeonRuneEffect effect,
        DungeonRuneTarget target,
        double bonus,
        String description
) {
    public DungeonRuneDefinition {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(displayName, "displayName");
        Objects.requireNonNull(material, "material");
        Objects.requireNonNull(rarity, "rarity");
        Objects.requireNonNull(effect, "effect");
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(description, "description");
        if (id.isBlank() || displayName.isBlank() || description.isBlank() || bonus <= 0.0D) {
            throw new IllegalArgumentException("Invalid dungeon rune definition.");
        }
    }
}
