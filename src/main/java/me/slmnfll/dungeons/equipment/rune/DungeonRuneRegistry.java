package me.slmnfll.dungeons.equipment.rune;

import me.slmnfll.dungeons.content.ItemRarity;
import org.bukkit.Material;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

public final class DungeonRuneRegistry {
    private final Map<String, DungeonRuneDefinition> byId = new LinkedHashMap<>();
    private final List<String> lootIds = List.of(
            "force", "force", "force", "force", "force", "force",
            "ward", "ward", "ward", "ward", "ward", "ward",
            "restoration", "restoration", "restoration", "restoration",
            "cinders", "cinders",
            "rift"
    );

    public DungeonRuneRegistry() {
        register(new DungeonRuneDefinition(
                "force", "Rune of Force", Material.ECHO_SHARD, ItemRarity.EPIC,
                DungeonRuneEffect.FORCE, DungeonRuneTarget.WEAPON, 0.05D,
                "Increases the weapon's damage by 5%."
        ));
        register(new DungeonRuneDefinition(
                "ward", "Rune of Warding", Material.PRISMARINE_CRYSTALS, ItemRarity.EPIC,
                DungeonRuneEffect.WARD, DungeonRuneTarget.ARMOR, 0.05D,
                "Increases the armor's Defense and Vitality by 5%."
        ));
        register(new DungeonRuneDefinition(
                "restoration", "Rune of Restoration", Material.QUARTZ, ItemRarity.EPIC,
                DungeonRuneEffect.RESTORATION, DungeonRuneTarget.EQUIPMENT, 1.0D,
                "Consumed to fully repair one dungeon equipment item."
        ));
        register(new DungeonRuneDefinition(
                "cinders", "Rune of Cinders", Material.BLAZE_POWDER, ItemRarity.LEGENDARY,
                DungeonRuneEffect.CINDERS, DungeonRuneTarget.WEAPON, 0.35D,
                "Every fifth hit releases a fiery burst around the target."
        ));
        register(new DungeonRuneDefinition(
                "rift", "Rune of the Rift", Material.ENDER_EYE, ItemRarity.MYTHICAL,
                DungeonRuneEffect.RIFT, DungeonRuneTarget.WEAPON, 5.5D,
                "Sneak-right-click to pull and slow nearby enemies."
        ));
    }

    public Optional<DungeonRuneDefinition> find(String id) {
        return Optional.ofNullable(byId.get(id));
    }

    public List<DungeonRuneDefinition> all() {
        return List.copyOf(byId.values());
    }

    public DungeonRuneDefinition rollLoot(Random random) {
        String id = lootIds.get(random.nextInt(lootIds.size()));
        return find(id).orElseThrow();
    }

    private void register(DungeonRuneDefinition definition) {
        if (byId.putIfAbsent(definition.id(), definition) != null) {
            throw new IllegalStateException("Duplicate dungeon rune ID: " + definition.id());
        }
    }
}
