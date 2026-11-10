package me.slmnfll.dungeons.equipment.consumable;

import me.slmnfll.dungeons.content.ItemRarity;
import org.bukkit.Color;
import org.bukkit.Material;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

public final class DungeonConsumableRegistry {
    private final Map<String, DungeonConsumableDefinition> byId = new LinkedHashMap<>();
    private final List<String> weightedLootIds = List.of(
            "mossbread", "mossbread", "mossbread",
            "sunroot", "sunroot",
            "deepdelve_stew", "lumen_berries",
            "healing", "stonehide", "cinder"
    );

    public DungeonConsumableRegistry() {
        register(new DungeonConsumableDefinition(
                "mossbread", "Mossbread", Material.BREAD, ItemRarity.COMMON,
                ConsumableAbility.REGENERATION, null,
                0.0D, 0.0D, 200, 60, 1
        ));
        register(new DungeonConsumableDefinition(
                "sunroot", "Roasted Sunroot", Material.BAKED_POTATO, ItemRarity.UNCOMMON,
                ConsumableAbility.REGENERATION, null,
                0.0D, 0.0D, 300, 80, 1
        ));
        register(new DungeonConsumableDefinition(
                "deepdelve_stew", "Deepdelve Stew", Material.MUSHROOM_STEW, ItemRarity.RARE,
                ConsumableAbility.REGENERATION, null,
                0.0D, 0.0D, 240, 100, 2
        ));
        register(new DungeonConsumableDefinition(
                "lumen_berries", "Lumen Berries", Material.GLOW_BERRIES, ItemRarity.RARE,
                ConsumableAbility.REGENERATION, null,
                0.0D, 0.0D, 120, 100, 3
        ));
        register(new DungeonConsumableDefinition(
                "healing", "Crimson Healing Phial", Material.POTION, ItemRarity.COMMON,
                ConsumableAbility.HEALING, Color.fromRGB(210, 45, 65),
                8.0D, 0.0D, 0, 60, 0
        ));
        register(new DungeonConsumableDefinition(
                "stonehide", "Stonehide Tonic", Material.POTION, ItemRarity.UNCOMMON,
                ConsumableAbility.STONEHIDE, Color.fromRGB(110, 115, 125),
                1.0D, 0.0D, 300, 80, 0
        ));
        register(new DungeonConsumableDefinition(
                "cinder", "Cinder Flask", Material.SPLASH_POTION, ItemRarity.RARE,
                ConsumableAbility.CINDER_BURST, Color.fromRGB(235, 105, 25),
                6.0D, 3.25D, 60, 100, 0
        ));
    }

    public Optional<DungeonConsumableDefinition> find(String id) {
        return Optional.ofNullable(byId.get(id));
    }

    public List<DungeonConsumableDefinition> all() {
        return List.copyOf(byId.values());
    }

    public DungeonConsumableDefinition rollLoot(Random random) {
        String id = weightedLootIds.get(random.nextInt(weightedLootIds.size()));
        return find(id).orElseThrow();
    }

    private void register(DungeonConsumableDefinition definition) {
        if (byId.putIfAbsent(definition.id(), definition) != null) {
            throw new IllegalStateException("Duplicate dungeon consumable ID: " + definition.id());
        }
    }
}
