package me.slmnfll.dungeons.equipment.armor;

import me.slmnfll.dungeons.affinity.Affinity;
import me.slmnfll.dungeons.content.ItemRarity;
import me.slmnfll.dungeons.equipment.rune.DungeonRuneCodec;
import me.slmnfll.dungeons.equipment.rune.DungeonRuneDefinition;
import me.slmnfll.dungeons.equipment.rune.DungeonRuneEffect;
import me.slmnfll.dungeons.equipment.rune.DungeonRuneRegistry;
import me.slmnfll.dungeons.profile.DungeonProfileService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class DungeonArmorService {
    public static final int ITEM_SCHEMA_VERSION = 1;
    private static final long SET_SEED_STEP = 0x9E3779B97F4A7C15L;

    private final DungeonArmorRegistry registry;
    private final DungeonRuneRegistry runeRegistry;
    private final DungeonProfileService profileService;
    private final NamespacedKey itemIdKey;
    private final NamespacedKey rarityKey;
    private final NamespacedKey schemaKey;
    private final NamespacedKey affinityKey;
    private final NamespacedKey setKey;
    private final NamespacedKey slotKey;
    private final NamespacedKey defenseKey;
    private final NamespacedKey vitalityKey;
    private final NamespacedKey powerKey;
    private final NamespacedKey maxDurabilityKey;
    private final NamespacedKey rollSeedKey;
    private final NamespacedKey socketedRunesKey;
    private final Map<DungeonArmorSlot, NamespacedKey> defenseModifierKeys = new EnumMap<>(DungeonArmorSlot.class);
    private final Map<DungeonArmorSlot, NamespacedKey> vitalityModifierKeys = new EnumMap<>(DungeonArmorSlot.class);

    public DungeonArmorService(
            JavaPlugin plugin,
            DungeonArmorRegistry registry,
            DungeonProfileService profileService,
            DungeonRuneRegistry runeRegistry
    ) {
        this.registry = Objects.requireNonNull(registry, "registry");
        this.profileService = Objects.requireNonNull(profileService, "profileService");
        this.runeRegistry = Objects.requireNonNull(runeRegistry, "runeRegistry");
        this.itemIdKey = new NamespacedKey(plugin, "item_id");
        this.rarityKey = new NamespacedKey(plugin, "rarity");
        this.schemaKey = new NamespacedKey(plugin, "item_schema");
        this.affinityKey = new NamespacedKey(plugin, "armor_affinity");
        this.setKey = new NamespacedKey(plugin, "armor_set");
        this.slotKey = new NamespacedKey(plugin, "armor_slot");
        this.defenseKey = new NamespacedKey(plugin, "armor_defense");
        this.vitalityKey = new NamespacedKey(plugin, "armor_vitality");
        this.powerKey = new NamespacedKey(plugin, "armor_power");
        this.maxDurabilityKey = new NamespacedKey(plugin, "armor_max_durability");
        this.rollSeedKey = new NamespacedKey(plugin, "armor_roll_seed");
        this.socketedRunesKey = new NamespacedKey(plugin, "socketed_runes");
        for (DungeonArmorSlot slot : DungeonArmorSlot.values()) {
            defenseModifierKeys.put(slot, new NamespacedKey(plugin, "armor_" + slot.id() + "_defense"));
            vitalityModifierKeys.put(slot, new NamespacedKey(plugin, "armor_" + slot.id() + "_vitality"));
        }
    }

    public ItemStack createLootArmor(Affinity affinity, DungeonArmorSlot slot, long seed) {
        return create(registry.piece(affinity, slot), seed);
    }

    public List<ItemStack> createSet(Affinity affinity, long seed) {
        List<ItemStack> items = new ArrayList<>();
        long pieceSeed = seed;
        for (DungeonArmorDefinition definition : registry.setFor(affinity)) {
            items.add(create(definition, pieceSeed));
            pieceSeed += SET_SEED_STEP;
        }
        return List.copyOf(items);
    }

    public ItemStack create(DungeonArmorDefinition definition, long seed) {
        DungeonArmorStats stats = ArmorRarityStatProfile.forRarity(definition.rarity()).roll(definition, seed);
        ItemStack item = new ItemStack(definition.material());
        apply(item, new DungeonArmorData(definition, stats, seed));
        return item;
    }

    public Optional<DungeonArmorData> read(ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) {
            return Optional.empty();
        }
        PersistentDataContainer data = item.getItemMeta().getPersistentDataContainer();
        String itemId = data.get(itemIdKey, PersistentDataType.STRING);
        DungeonArmorDefinition definition = itemId == null ? null : registry.find(itemId).orElse(null);
        Double defense = data.get(defenseKey, PersistentDataType.DOUBLE);
        Double vitality = data.get(vitalityKey, PersistentDataType.DOUBLE);
        Double power = data.get(powerKey, PersistentDataType.DOUBLE);
        Integer maxDurability = data.get(maxDurabilityKey, PersistentDataType.INTEGER);
        Long rollSeed = data.get(rollSeedKey, PersistentDataType.LONG);
        if (definition == null || defense == null || vitality == null || power == null
                || maxDurability == null || rollSeed == null) {
            return Optional.empty();
        }
        return Optional.of(new DungeonArmorData(
                definition,
                new DungeonArmorStats(defense, vitality, power, maxDurability),
                rollSeed
        ));
    }

    public DungeonArmorSummary summary(Player player) {
        Affinity selectedAffinity = profileService.profile(player).selectedAffinity().orElse(null);
        double powerBonus = 0.0D;
        Map<DungeonArmorSet, Integer> equippedPieces = new EnumMap<>(DungeonArmorSet.class);
        for (ItemStack item : player.getInventory().getArmorContents()) {
            DungeonArmorData armor = read(item).orElse(null);
            if (armor == null) {
                continue;
            }
            double piecePower = armor.stats().power();
            if (armor.definition().affinity() == selectedAffinity) {
                piecePower *= 1.0D + Affinity.EFFECTIVENESS_BONUS;
            }
            powerBonus += piecePower;
            equippedPieces.merge(armor.definition().set(), 1, Integer::sum);
        }
        return new DungeonArmorSummary(powerBonus, equippedPieces);
    }

    public void refresh(ItemStack item) {
        DungeonArmorData armor = read(item).orElseThrow(() ->
                new IllegalArgumentException("That item is not dungeon armor."));
        apply(item, armor);
    }

    private void apply(ItemStack item, DungeonArmorData armor) {
        DungeonArmorDefinition definition = armor.definition();
        DungeonArmorStats stats = armor.stats();
        List<DungeonRuneDefinition> runes = socketedRunes(item);
        double guardMultiplier = 1.0D + DungeonRuneCodec.bonus(runes, DungeonRuneEffect.WARD);
        double effectiveDefense = stats.defense() * guardMultiplier;
        double effectiveVitality = stats.vitality() * guardMultiplier;
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(definition.displayName(), definition.rarity().textColor())
                .decorate(TextDecoration.BOLD)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(buildLore(armor, runes, effectiveDefense, effectiveVitality));
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_UNBREAKABLE);

        PersistentDataContainer data = meta.getPersistentDataContainer();
        data.set(itemIdKey, PersistentDataType.STRING, definition.id());
        data.set(rarityKey, PersistentDataType.STRING, definition.rarity().name());
        data.set(schemaKey, PersistentDataType.INTEGER, ITEM_SCHEMA_VERSION);
        data.set(affinityKey, PersistentDataType.STRING, definition.affinity().id());
        data.set(setKey, PersistentDataType.STRING, definition.set().id());
        data.set(slotKey, PersistentDataType.STRING, definition.slot().id());
        data.set(defenseKey, PersistentDataType.DOUBLE, stats.defense());
        data.set(vitalityKey, PersistentDataType.DOUBLE, stats.vitality());
        data.set(powerKey, PersistentDataType.DOUBLE, stats.power());
        data.set(maxDurabilityKey, PersistentDataType.INTEGER, stats.maxDurability());
        data.set(rollSeedKey, PersistentDataType.LONG, armor.rollSeed());

        meta.removeAttributeModifier(Attribute.ARMOR);
        meta.removeAttributeModifier(Attribute.MAX_HEALTH);
        meta.addAttributeModifier(Attribute.ARMOR, new AttributeModifier(
                defenseModifierKeys.get(definition.slot()),
                effectiveDefense,
                AttributeModifier.Operation.ADD_NUMBER,
                definition.slot().equipmentSlotGroup()
        ));
        if (effectiveVitality > 0.0D) {
            meta.addAttributeModifier(Attribute.MAX_HEALTH, new AttributeModifier(
                    vitalityModifierKeys.get(definition.slot()),
                    effectiveVitality,
                    AttributeModifier.Operation.ADD_NUMBER,
                    definition.slot().equipmentSlotGroup()
            ));
        }

        if (meta instanceof Damageable damageable) {
            if (stats.maxDurability() == 0) {
                damageable.setUnbreakable(true);
            } else {
                damageable.setMaxDamage(stats.maxDurability());
            }
        }
        int unbreaking = unbreakingLevel(definition.rarity());
        if (unbreaking > 0) {
            meta.addEnchant(Enchantment.UNBREAKING, unbreaking, true);
        }
        item.setItemMeta(meta);
    }

    private List<Component> buildLore(
            DungeonArmorData armor,
            List<DungeonRuneDefinition> runes,
            double effectiveDefense,
            double effectiveVitality
    ) {
        DungeonArmorDefinition definition = armor.definition();
        DungeonArmorStats stats = armor.stats();
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text(definition.rarity().displayName(), definition.rarity().textColor())
                .decorate(TextDecoration.BOLD)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        lore.add(statLine("Set", definition.set().displayName(), NamedTextColor.LIGHT_PURPLE));
        lore.add(statLine("Affinity", definition.affinity().displayName(), NamedTextColor.AQUA));
        lore.add(statLine("Defense", String.format(Locale.ROOT, "%.1f", effectiveDefense), NamedTextColor.BLUE));
        lore.add(statLine("Vitality", "+" + String.format(Locale.ROOT, "%.1f Health", effectiveVitality), NamedTextColor.RED));
        lore.add(statLine("Power", "+" + String.format(Locale.ROOT, "%.1f%%", stats.power() * 100.0D), NamedTextColor.GOLD));
        lore.add(statLine(
                "Durability",
                stats.maxDurability() == 0 ? "Infinite" : Integer.toString(stats.maxDurability()),
                NamedTextColor.GREEN
        ));
        lore.add(Component.empty());
        lore.add(Component.text("Set Bonuses", NamedTextColor.DARK_PURPLE)
                .decorate(TextDecoration.BOLD)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(setBonusLine("2 Pieces", definition.set().twoPieceDescription()));
        lore.add(setBonusLine("4 Pieces", definition.set().fourPieceDescription()));
        appendRuneLore(lore, runes);
        return List.copyOf(lore);
    }

    private List<DungeonRuneDefinition> socketedRunes(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return List.of();
        }
        String encoded = item.getItemMeta().getPersistentDataContainer()
                .get(socketedRunesKey, PersistentDataType.STRING);
        return DungeonRuneCodec.decode(encoded, runeRegistry);
    }

    private void appendRuneLore(List<Component> lore, List<DungeonRuneDefinition> runes) {
        lore.add(Component.empty());
        lore.add(Component.text(
                        "Rune Sockets (" + runes.size() + "/" + DungeonRuneCodec.MAX_SOCKETS + ")",
                        NamedTextColor.DARK_AQUA
                )
                .decorate(TextDecoration.BOLD)
                .decoration(TextDecoration.ITALIC, false));
        if (runes.isEmpty()) {
            lore.add(Component.text("Empty", NamedTextColor.DARK_GRAY)
                    .decoration(TextDecoration.ITALIC, false));
            return;
        }
        for (DungeonRuneDefinition rune : runes) {
            lore.add(Component.text("• " + rune.displayName(), rune.rarity().textColor())
                    .decoration(TextDecoration.ITALIC, false));
        }
    }

    private Component statLine(String label, String value, NamedTextColor valueColor) {
        return Component.text(label + ": ", NamedTextColor.GRAY)
                .append(Component.text(value, valueColor))
                .decoration(TextDecoration.ITALIC, false);
    }

    private Component setBonusLine(String threshold, String description) {
        return Component.text(threshold + ": ", NamedTextColor.GRAY)
                .append(Component.text(description, NamedTextColor.DARK_AQUA))
                .decoration(TextDecoration.ITALIC, false);
    }

    private int unbreakingLevel(ItemRarity rarity) {
        return switch (rarity) {
            case COMMON, UNCOMMON, RARE -> 1;
            case EPIC -> 2;
            case LEGENDARY -> 3;
            case MYTHICAL -> 5;
            case EXTINCT -> 0;
        };
    }
}
