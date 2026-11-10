package me.slmnfll.dungeons.equipment.armor;

import me.slmnfll.dungeons.affinity.Affinity;
import me.slmnfll.dungeons.content.ItemRarity;
import org.bukkit.Material;

import java.util.Objects;

public record DungeonArmorDefinition(
        String id,
        String displayName,
        DungeonArmorSet set,
        DungeonArmorSlot slot,
        Material material,
        ItemRarity rarity,
        double baseDefense,
        double baseVitality,
        double basePower,
        int baseDurability,
        boolean fixedStats
) {
    public DungeonArmorDefinition {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(displayName, "displayName");
        Objects.requireNonNull(set, "set");
        Objects.requireNonNull(slot, "slot");
        Objects.requireNonNull(material, "material");
        Objects.requireNonNull(rarity, "rarity");
        if (id.isBlank() || displayName.isBlank() || baseDefense < 0.0D || baseVitality < 0.0D
                || basePower < 0.0D || baseDurability < 1) {
            throw new IllegalArgumentException("Invalid dungeon armor definition.");
        }
    }

    public Affinity affinity() {
        return set.affinity();
    }
}
