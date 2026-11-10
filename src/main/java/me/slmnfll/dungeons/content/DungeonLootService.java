package me.slmnfll.dungeons.content;

import me.slmnfll.dungeons.affinity.Affinity;
import me.slmnfll.dungeons.equipment.armor.DungeonArmorService;
import me.slmnfll.dungeons.equipment.armor.DungeonArmorSlot;
import me.slmnfll.dungeons.equipment.artifact.DungeonArtifactService;
import me.slmnfll.dungeons.equipment.consumable.DungeonConsumableService;
import me.slmnfll.dungeons.equipment.rune.DungeonRuneService;
import me.slmnfll.dungeons.equipment.weapon.DungeonWeaponService;
import me.slmnfll.dungeons.profile.DungeonProgressionBenefits;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class DungeonLootService {
    private static final List<LootEntry> POT_LOOT = List.of(
            new LootEntry("dungeon_coin", "Dungeon Coin", Material.GOLD_NUGGET, ItemRarity.COMMON, 1, 4, 420),
            new LootEntry("sturdy_arrow", "Sturdy Arrow", Material.ARROW, ItemRarity.COMMON, 2, 6, 260),
            new LootEntry("dungeon_rations", "Dungeon Rations", Material.BREAD, ItemRarity.COMMON, 1, 2, 180),
            new LootEntry("polished_emerald", "Polished Emerald", Material.EMERALD, ItemRarity.UNCOMMON, 1, 2, 100),
            new LootEntry("jewelry_fragment", "Jewelry Fragment", Material.AMETHYST_SHARD, ItemRarity.RARE, 1, 1, 35),
            new LootEntry("ancient_ring", "Ancient Ring", Material.ECHO_SHARD, ItemRarity.EPIC, 1, 1, 5),
            new LootEntry("royal_signet", "Royal Signet", Material.HEART_OF_THE_SEA, ItemRarity.LEGENDARY, 1, 1, 1)
    );

    private static final List<LootEntry> CHEST_LOOT = List.of(
            new LootEntry("jewelry_fragment", "Jewelry Fragment", Material.AMETHYST_SHARD, ItemRarity.RARE, 1, 3, 55),
            new LootEntry("ancient_ring", "Ancient Ring", Material.ECHO_SHARD, ItemRarity.EPIC, 1, 1, 30),
            new LootEntry("royal_signet", "Royal Signet", Material.HEART_OF_THE_SEA, ItemRarity.LEGENDARY, 1, 1, 12),
            new LootEntry("fallen_star", "Fallen Star", Material.NETHER_STAR, ItemRarity.MYTHICAL, 1, 1, 3)
    );
    private static final LootEntry TUTORIAL_SCRAP = new LootEntry(
            "aevori_scrap", "Aevori Scrap", Material.IRON_NUGGET, ItemRarity.COMMON, 1, 1, 1
    );
    private static final LootEntry TUTORIAL_WARDEN_FRAGMENT = new LootEntry(
            "survey_warden_fragment", "Survey Warden Fragment", Material.ECHO_SHARD,
            ItemRarity.RARE, 1, 1, 1
    );

    private final NamespacedKey itemIdKey;
    private final NamespacedKey rarityKey;
    private final DungeonWeaponService weaponService;
    private final DungeonArmorService armorService;
    private final DungeonArtifactService artifactService;
    private final DungeonRuneService runeService;
    private final DungeonConsumableService consumableService;
    private final DungeonProgressionBenefits progressionBenefits = new DungeonProgressionBenefits();

    public DungeonLootService(
            JavaPlugin plugin,
            DungeonWeaponService weaponService,
            DungeonArmorService armorService,
            DungeonArtifactService artifactService,
            DungeonRuneService runeService,
            DungeonConsumableService consumableService
    ) {
        this.itemIdKey = new NamespacedKey(plugin, "item_id");
        this.rarityKey = new NamespacedKey(plugin, "rarity");
        this.weaponService = weaponService;
        this.armorService = armorService;
        this.artifactService = artifactService;
        this.runeService = runeService;
        this.consumableService = consumableService;
    }

    public List<ItemStack> rollPotLoot(PotDefinition pot, Affinity preferredAffinity, int dungeonLevel) {
        Random random = new Random(pot.lootSeed());
        int rolls = random.nextDouble() < 0.28D ? 2 : 1;
        List<ItemStack> results = new ArrayList<>();
        for (int roll = 0; roll < rolls; roll++) {
            LootEntry entry = select(POT_LOOT, random, pot.boostedRarity());
            results.add(createItem(entry, entry.rollAmount(random)));
        }
        double weaponChance = pot.boostedRarity() == ItemRarity.RARE ? 0.14D : 0.015D;
        addWeaponIfRolled(results, random, preferredAffinity, adjustedSpecialChance(weaponChance, dungeonLevel));
        double armorChance = pot.boostedRarity() == ItemRarity.RARE ? 0.08D : 0.005D;
        addArmorIfRolled(results, random, preferredAffinity, adjustedSpecialChance(armorChance, dungeonLevel));
        double artifactChance = pot.boostedRarity() == ItemRarity.RARE ? 0.035D : 0.002D;
        addArtifactIfRolled(results, random, preferredAffinity, adjustedSpecialChance(artifactChance, dungeonLevel));
        double runeChance = pot.boostedRarity() == ItemRarity.RARE ? 0.04D : 0.003D;
        addRuneIfRolled(results, random, adjustedSpecialChance(runeChance, dungeonLevel));
        double consumableChance = pot.boostedRarity() == ItemRarity.RARE ? 0.18D : 0.08D;
        addConsumableIfRolled(results, random, consumableChance);
        return results;
    }

    public List<ItemStack> tutorialPotLoot(PotDefinition pot) {
        if (pot.lootSeed() == 0x5101L) {
            return List.of(consumableService.create("mossbread"));
        }
        if (pot.lootSeed() == 0x5102L) {
            return List.of(createItem(requiredEntry(POT_LOOT, "jewelry_fragment"), 1));
        }
        if (pot.lootSeed() == 0x5201L) {
            return List.of(createItem(requiredEntry(POT_LOOT, "sturdy_arrow"), 4));
        }
        if (pot.lootSeed() == 0x5202L) {
            return List.of(runeService.createLootRune(new Random(pot.lootSeed())));
        }
        return List.of(createItem(TUTORIAL_SCRAP, 1));
    }

    public List<ItemStack> tutorialChestLoot(LockedChestDefinition chest, Affinity affinity) {
        return List.of(
                weaponService.createLootWeapon(affinity, chest.lootSeed()),
                consumableService.create("sunroot")
        );
    }

    public List<ItemStack> tutorialEncounterLoot(long seed, boolean boss) {
        if (boss) {
            return List.of(createItem(TUTORIAL_WARDEN_FRAGMENT, 1));
        }
        return List.of(consumableService.create("deepdelve_stew"));
    }

    public List<ItemStack> rollChestLoot(
            LockedChestDefinition chest,
            Affinity preferredAffinity,
            int dungeonLevel
    ) {
        Random random = new Random(chest.lootSeed());
        int rolls = 3 + random.nextInt(3);
        List<ItemStack> results = new ArrayList<>();
        for (int roll = 0; roll < rolls; roll++) {
            LootEntry entry = select(CHEST_LOOT, random, null);
            results.add(createItem(entry, entry.rollAmount(random)));
        }
        addWeaponIfRolled(results, random, preferredAffinity, adjustedSpecialChance(0.22D, dungeonLevel));
        addArmorIfRolled(results, random, preferredAffinity, adjustedSpecialChance(0.20D, dungeonLevel));
        addArtifactIfRolled(results, random, preferredAffinity, adjustedSpecialChance(0.12D, dungeonLevel));
        addRuneIfRolled(results, random, adjustedSpecialChance(0.10D, dungeonLevel));
        addConsumableIfRolled(results, random, 0.30D);
        return results;
    }

    public List<ItemStack> rollEncounterLoot(
            long seed,
            boolean optional,
            boolean boss,
            Affinity preferredAffinity,
            int dungeonLevel
    ) {
        Random random = new Random(seed);
        List<LootEntry> table = boss ? CHEST_LOOT : POT_LOOT;
        int rolls = boss ? 5 : optional ? 2 : 1;
        ItemRarity boost = boss ? null : optional ? ItemRarity.RARE : ItemRarity.UNCOMMON;
        List<ItemStack> results = new ArrayList<>();
        for (int roll = 0; roll < rolls; roll++) {
            LootEntry entry = select(table, random, boost);
            results.add(createItem(entry, entry.rollAmount(random)));
        }
        addWeaponIfRolled(results, random, preferredAffinity,
                adjustedSpecialChance(boss ? 0.55D : optional ? 0.18D : 0.05D, dungeonLevel));
        addArmorIfRolled(results, random, preferredAffinity,
                adjustedSpecialChance(boss ? 0.55D : optional ? 0.15D : 0.03D, dungeonLevel));
        addArtifactIfRolled(results, random, preferredAffinity,
                adjustedSpecialChance(boss ? 0.35D : optional ? 0.08D : 0.01D, dungeonLevel));
        addRuneIfRolled(results, random,
                adjustedSpecialChance(boss ? 0.30D : optional ? 0.05D : 0.01D, dungeonLevel));
        addConsumableIfRolled(results, random, boss ? 0.55D : optional ? 0.20D : 0.08D);
        return results;
    }

    private void addWeaponIfRolled(
            List<ItemStack> results,
            Random random,
            Affinity preferredAffinity,
            double chance
    ) {
        if (random.nextDouble() >= chance) {
            return;
        }
        Affinity rolledAffinity = random.nextDouble() < 0.70D
                ? preferredAffinity
                : Affinity.values()[random.nextInt(Affinity.values().length)];
        results.add(weaponService.createLootWeapon(rolledAffinity, random.nextLong()));
    }

    private void addArmorIfRolled(
            List<ItemStack> results,
            Random random,
            Affinity preferredAffinity,
            double chance
    ) {
        if (random.nextDouble() >= chance) {
            return;
        }
        Affinity rolledAffinity = random.nextDouble() < 0.70D
                ? preferredAffinity
                : Affinity.values()[random.nextInt(Affinity.values().length)];
        DungeonArmorSlot slot = DungeonArmorSlot.values()[random.nextInt(DungeonArmorSlot.values().length)];
        results.add(armorService.createLootArmor(rolledAffinity, slot, random.nextLong()));
    }

    private void addArtifactIfRolled(
            List<ItemStack> results,
            Random random,
            Affinity preferredAffinity,
            double chance
    ) {
        if (random.nextDouble() >= chance) {
            return;
        }
        Affinity rolledAffinity = random.nextDouble() < 0.70D
                ? preferredAffinity
                : Affinity.values()[random.nextInt(Affinity.values().length)];
        results.add(artifactService.createLootArtifact(rolledAffinity));
    }

    private void addRuneIfRolled(List<ItemStack> results, Random random, double chance) {
        if (random.nextDouble() < chance) {
            results.add(runeService.createLootRune(random));
        }
    }

    private void addConsumableIfRolled(List<ItemStack> results, Random random, double chance) {
        if (random.nextDouble() < chance) {
            results.add(consumableService.createLootConsumable(random));
        }
    }

    private double adjustedSpecialChance(double baseChance, int dungeonLevel) {
        return Math.min(1.0D, baseChance * progressionBenefits.specialLootChanceMultiplier(dungeonLevel));
    }

    private LootEntry select(List<LootEntry> table, Random random, ItemRarity boostedRarity) {
        int totalWeight = table.stream().mapToInt(entry -> adjustedWeight(entry, boostedRarity)).sum();
        int selectedWeight = random.nextInt(totalWeight);
        for (LootEntry entry : table) {
            selectedWeight -= adjustedWeight(entry, boostedRarity);
            if (selectedWeight < 0) {
                return entry;
            }
        }
        throw new IllegalStateException("The loot table has no selectable entry.");
    }

    private int adjustedWeight(LootEntry entry, ItemRarity boostedRarity) {
        if (boostedRarity == null || entry.rarity() != boostedRarity) {
            return entry.weight();
        }
        int multiplier = switch (boostedRarity) {
            case UNCOMMON -> 4;
            case RARE -> 8;
            case EPIC -> 30;
            case LEGENDARY -> 80;
            default -> 1;
        };
        return entry.weight() * multiplier;
    }

    private ItemStack createItem(LootEntry entry, int amount) {
        ItemStack item = new ItemStack(entry.material(), amount);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(entry.displayName(), entry.rarity().textColor())
                .decorate(TextDecoration.BOLD)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(
                Component.text(entry.rarity().displayName(), entry.rarity().textColor())
                        .decorate(TextDecoration.BOLD)
                        .decoration(TextDecoration.ITALIC, false),
                Component.text("Dungeon loot", NamedTextColor.DARK_GRAY)
                        .decoration(TextDecoration.ITALIC, false)
        ));
        meta.getPersistentDataContainer().set(itemIdKey, PersistentDataType.STRING, entry.id());
        meta.getPersistentDataContainer().set(rarityKey, PersistentDataType.STRING, entry.rarity().name());
        item.setItemMeta(meta);
        return item;
    }

    private LootEntry requiredEntry(List<LootEntry> entries, String id) {
        return entries.stream()
                .filter(entry -> entry.id().equals(id))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Missing loot entry: " + id));
    }
}
