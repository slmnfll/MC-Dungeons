package me.slmnfll.dungeons.equipment.armor;

import me.slmnfll.dungeons.affinity.Affinity;
import me.slmnfll.dungeons.content.ItemRarity;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class DungeonArmorRegistry {
    private final Map<String, DungeonArmorDefinition> byId = new LinkedHashMap<>();
    private final Map<Affinity, Map<DungeonArmorSlot, DungeonArmorDefinition>> byAffinity =
            new EnumMap<>(Affinity.class);

    public DungeonArmorRegistry() {
        registerSet(
                DungeonArmorSet.BULWARK,
                "Bulwark",
                2.8D, 5.2D, 4.2D, 2.8D,
                1.5D, 3.0D, 2.0D, 1.5D,
                0.004D, 0.006D, 0.006D, 0.004D
        );
        registerSet(
                DungeonArmorSet.TRAILSEEKER,
                "Trailseeker",
                2.2D, 4.2D, 3.4D, 2.2D,
                1.0D, 2.0D, 1.5D, 1.0D,
                0.008D, 0.012D, 0.012D, 0.008D
        );
        registerSet(
                DungeonArmorSet.WARBORN,
                "Warborn",
                2.4D, 4.5D, 3.6D, 2.4D,
                0.75D, 1.5D, 1.0D, 0.75D,
                0.012D, 0.018D, 0.018D, 0.012D
        );
    }

    public Optional<DungeonArmorDefinition> find(String id) {
        return Optional.ofNullable(byId.get(id));
    }

    public DungeonArmorDefinition piece(Affinity affinity, DungeonArmorSlot slot) {
        Map<DungeonArmorSlot, DungeonArmorDefinition> pieces = byAffinity.get(affinity);
        if (pieces == null || !pieces.containsKey(slot)) {
            throw new IllegalStateException("No " + slot.id() + " registered for " + affinity.id());
        }
        return pieces.get(slot);
    }

    public List<DungeonArmorDefinition> setFor(Affinity affinity) {
        List<DungeonArmorDefinition> results = new ArrayList<>();
        for (DungeonArmorSlot slot : DungeonArmorSlot.values()) {
            results.add(piece(affinity, slot));
        }
        return List.copyOf(results);
    }

    private void registerSet(
            DungeonArmorSet set,
            String prefix,
            double helmetDefense,
            double chestDefense,
            double legsDefense,
            double bootsDefense,
            double helmetVitality,
            double chestVitality,
            double legsVitality,
            double bootsVitality,
            double helmetPower,
            double chestPower,
            double legsPower,
            double bootsPower
    ) {
        register(piece(set, prefix, DungeonArmorSlot.HELMET, Material.DIAMOND_HELMET,
                helmetDefense, helmetVitality, helmetPower, 360));
        register(piece(set, prefix, DungeonArmorSlot.CHESTPLATE, Material.DIAMOND_CHESTPLATE,
                chestDefense, chestVitality, chestPower, 520));
        register(piece(set, prefix, DungeonArmorSlot.LEGGINGS, Material.DIAMOND_LEGGINGS,
                legsDefense, legsVitality, legsPower, 480));
        register(piece(set, prefix, DungeonArmorSlot.BOOTS, Material.DIAMOND_BOOTS,
                bootsDefense, bootsVitality, bootsPower, 420));
    }

    private DungeonArmorDefinition piece(
            DungeonArmorSet set,
            String prefix,
            DungeonArmorSlot slot,
            Material material,
            double defense,
            double vitality,
            double power,
            int durability
    ) {
        return new DungeonArmorDefinition(
                set.id() + "_" + slot.id(),
                prefix + " " + slot.displayName(),
                set,
                slot,
                material,
                ItemRarity.RARE,
                defense,
                vitality,
                power,
                durability,
                false
        );
    }

    private void register(DungeonArmorDefinition definition) {
        if (byId.putIfAbsent(definition.id(), definition) != null) {
            throw new IllegalStateException("Duplicate dungeon armor ID: " + definition.id());
        }
        Map<DungeonArmorSlot, DungeonArmorDefinition> pieces = byAffinity.computeIfAbsent(
                definition.affinity(),
                ignored -> new EnumMap<>(DungeonArmorSlot.class)
        );
        if (pieces.putIfAbsent(definition.slot(), definition) != null) {
            throw new IllegalStateException(
                    "Duplicate " + definition.slot().id() + " for " + definition.affinity().id()
            );
        }
    }
}
