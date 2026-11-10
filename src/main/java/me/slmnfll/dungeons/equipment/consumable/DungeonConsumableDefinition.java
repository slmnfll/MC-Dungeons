package me.slmnfll.dungeons.equipment.consumable;

import me.slmnfll.dungeons.content.ItemRarity;
import org.bukkit.Color;
import org.bukkit.Material;

import java.util.Objects;

public record DungeonConsumableDefinition(
        String id,
        String displayName,
        Material material,
        ItemRarity rarity,
        ConsumableAbility ability,
        Color color,
        double magnitude,
        double radius,
        int durationTicks,
        int cooldownTicks,
        int regenerationLevel
) {
    public DungeonConsumableDefinition {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(displayName, "displayName");
        Objects.requireNonNull(material, "material");
        Objects.requireNonNull(rarity, "rarity");
        Objects.requireNonNull(ability, "ability");
        if (id.isBlank() || displayName.isBlank() || magnitude < 0.0D || radius < 0.0D
                || durationTicks < 0 || cooldownTicks < 1
                || regenerationLevel < 0 || regenerationLevel > 3) {
            throw new IllegalArgumentException("Invalid dungeon consumable definition.");
        }
        if (ability == ConsumableAbility.REGENERATION && regenerationLevel == 0) {
            throw new IllegalArgumentException("A regeneration consumable needs an effect level.");
        }
    }

    public boolean throwable() {
        return material == Material.SPLASH_POTION;
    }

    public boolean food() {
        return material != Material.POTION && material != Material.SPLASH_POTION;
    }
}
