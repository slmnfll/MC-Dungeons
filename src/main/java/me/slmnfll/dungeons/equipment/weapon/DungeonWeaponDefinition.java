package me.slmnfll.dungeons.equipment.weapon;

import me.slmnfll.dungeons.affinity.Affinity;
import me.slmnfll.dungeons.content.ItemRarity;
import org.bukkit.Material;

import java.util.Objects;

public record DungeonWeaponDefinition(
        String id,
        String displayName,
        Affinity affinity,
        Material material,
        ItemRarity rarity,
        double baseDamage,
        double baseSpeed,
        int baseDurability,
        WeaponAbility ability,
        boolean fixedStats
) {
    public DungeonWeaponDefinition {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(displayName, "displayName");
        Objects.requireNonNull(affinity, "affinity");
        Objects.requireNonNull(material, "material");
        Objects.requireNonNull(rarity, "rarity");
        Objects.requireNonNull(ability, "ability");
        if (id.isBlank() || displayName.isBlank() || baseDamage <= 0.0D || baseSpeed <= 0.0D || baseDurability < 1) {
            throw new IllegalArgumentException("Invalid dungeon weapon definition.");
        }
    }
}
