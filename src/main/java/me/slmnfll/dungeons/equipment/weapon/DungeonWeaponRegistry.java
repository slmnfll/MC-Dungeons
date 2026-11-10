package me.slmnfll.dungeons.equipment.weapon;

import me.slmnfll.dungeons.affinity.Affinity;
import me.slmnfll.dungeons.content.ItemRarity;
import org.bukkit.Material;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class DungeonWeaponRegistry {
    private final Map<String, DungeonWeaponDefinition> byId = new LinkedHashMap<>();
    private final Map<Affinity, DungeonWeaponDefinition> starters = new EnumMap<>(Affinity.class);
    private final Map<Affinity, DungeonWeaponDefinition> firstLootWeapons = new EnumMap<>(Affinity.class);

    public DungeonWeaponRegistry() {
        registerStarter(new DungeonWeaponDefinition(
                "starter_vanguard_weapon", "Vanguard Starter Sword", Affinity.VANGUARD,
                Material.IRON_SWORD, ItemRarity.COMMON, 6.0D, 1.60D, 250, WeaponAbility.NONE, true
        ));
        registerStarter(new DungeonWeaponDefinition(
                "starter_arbalist_weapon", "Arbalist Starter Crossbow", Affinity.ARBALIST,
                Material.CROSSBOW, ItemRarity.COMMON, 6.0D, 0.80D, 326, WeaponAbility.NONE, true
        ));
        registerStarter(new DungeonWeaponDefinition(
                "starter_ravager_weapon", "Ravager Starter Axe", Affinity.RAVAGER,
                Material.IRON_AXE, ItemRarity.COMMON, 9.0D, 0.90D, 250, WeaponAbility.NONE, true
        ));

        registerLoot(new DungeonWeaponDefinition(
                "cryptblade", "Cryptblade", Affinity.VANGUARD,
                Material.DIAMOND_SWORD, ItemRarity.RARE, 6.5D, 1.55D, 450, WeaponAbility.CLEAVE, false
        ));
        registerLoot(new DungeonWeaponDefinition(
                "bonepiercer", "Bonepiercer", Affinity.ARBALIST,
                Material.CROSSBOW, ItemRarity.RARE, 7.5D, 0.90D, 520, WeaponAbility.HUNTERS_MARK, false
        ));
        registerLoot(new DungeonWeaponDefinition(
                "gravebreaker", "Gravebreaker", Affinity.RAVAGER,
                Material.DIAMOND_AXE, ItemRarity.RARE, 9.5D, 0.86D, 475, WeaponAbility.SHOCKWAVE, false
        ));
    }

    public Optional<DungeonWeaponDefinition> find(String id) {
        return Optional.ofNullable(byId.get(id));
    }

    public DungeonWeaponDefinition starterFor(Affinity affinity) {
        return required(starters, affinity, "starter");
    }

    public DungeonWeaponDefinition firstLootWeaponFor(Affinity affinity) {
        return required(firstLootWeapons, affinity, "loot weapon");
    }

    private void registerStarter(DungeonWeaponDefinition definition) {
        register(definition);
        starters.put(definition.affinity(), definition);
    }

    private void registerLoot(DungeonWeaponDefinition definition) {
        register(definition);
        firstLootWeapons.put(definition.affinity(), definition);
    }

    private void register(DungeonWeaponDefinition definition) {
        if (byId.putIfAbsent(definition.id(), definition) != null) {
            throw new IllegalStateException("Duplicate dungeon weapon ID: " + definition.id());
        }
    }

    private DungeonWeaponDefinition required(
            Map<Affinity, DungeonWeaponDefinition> definitions,
            Affinity affinity,
            String kind
    ) {
        DungeonWeaponDefinition definition = definitions.get(affinity);
        if (definition == null) {
            throw new IllegalStateException("No " + kind + " registered for " + affinity.id());
        }
        return definition;
    }
}
