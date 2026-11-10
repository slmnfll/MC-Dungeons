package me.slmnfll.dungeons.equipment.weapon;

import me.slmnfll.dungeons.affinity.Affinity;
import me.slmnfll.dungeons.content.ItemRarity;
import me.slmnfll.dungeons.equipment.rune.DungeonRuneCodec;
import me.slmnfll.dungeons.equipment.rune.DungeonRuneDefinition;
import me.slmnfll.dungeons.equipment.rune.DungeonRuneEffect;
import me.slmnfll.dungeons.equipment.rune.DungeonRuneRegistry;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class DungeonWeaponService {
    public static final int ITEM_SCHEMA_VERSION = 1;

    private final DungeonWeaponRegistry registry;
    private final DungeonRuneRegistry runeRegistry;
    private final NamespacedKey itemIdKey;
    private final NamespacedKey rarityKey;
    private final NamespacedKey schemaKey;
    private final NamespacedKey affinityKey;
    private final NamespacedKey damageKey;
    private final NamespacedKey speedKey;
    private final NamespacedKey maxDurabilityKey;
    private final NamespacedKey abilityKey;
    private final NamespacedKey abilityRevealedKey;
    private final NamespacedKey rollSeedKey;
    private final NamespacedKey socketedRunesKey;
    private final NamespacedKey attackDamageModifierKey;
    private final NamespacedKey attackSpeedModifierKey;

    public DungeonWeaponService(
            JavaPlugin plugin,
            DungeonWeaponRegistry registry,
            DungeonRuneRegistry runeRegistry
    ) {
        this.registry = registry;
        this.runeRegistry = runeRegistry;
        this.itemIdKey = new NamespacedKey(plugin, "item_id");
        this.rarityKey = new NamespacedKey(plugin, "rarity");
        this.schemaKey = new NamespacedKey(plugin, "item_schema");
        this.affinityKey = new NamespacedKey(plugin, "weapon_affinity");
        this.damageKey = new NamespacedKey(plugin, "weapon_damage");
        this.speedKey = new NamespacedKey(plugin, "weapon_speed");
        this.maxDurabilityKey = new NamespacedKey(plugin, "weapon_max_durability");
        this.abilityKey = new NamespacedKey(plugin, "weapon_ability");
        this.abilityRevealedKey = new NamespacedKey(plugin, "ability_revealed");
        this.rollSeedKey = new NamespacedKey(plugin, "weapon_roll_seed");
        this.socketedRunesKey = new NamespacedKey(plugin, "socketed_runes");
        this.attackDamageModifierKey = new NamespacedKey(plugin, "custom_weapon_damage");
        this.attackSpeedModifierKey = new NamespacedKey(plugin, "custom_weapon_speed");
    }

    public ItemStack createStarter(Affinity affinity) {
        DungeonWeaponDefinition definition = registry.starterFor(affinity);
        return create(definition, 0L, false);
    }

    public ItemStack createLootWeapon(Affinity affinity, long seed) {
        return create(registry.firstLootWeaponFor(affinity), seed, false);
    }

    public ItemStack create(DungeonWeaponDefinition definition, long seed, boolean abilityRevealed) {
        DungeonWeaponStats stats = RarityStatProfile.forRarity(definition.rarity()).roll(definition, seed);
        ItemStack item = new ItemStack(definition.material());
        apply(item, new DungeonWeaponData(definition, stats, seed, abilityRevealed));
        return item;
    }

    public Optional<DungeonWeaponData> read(ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) {
            return Optional.empty();
        }
        PersistentDataContainer data = item.getItemMeta().getPersistentDataContainer();
        String itemId = data.get(itemIdKey, PersistentDataType.STRING);
        DungeonWeaponDefinition definition = itemId == null ? null : registry.find(itemId).orElse(null);
        Double damage = data.get(damageKey, PersistentDataType.DOUBLE);
        Double speed = data.get(speedKey, PersistentDataType.DOUBLE);
        Integer maxDurability = data.get(maxDurabilityKey, PersistentDataType.INTEGER);
        Long rollSeed = data.get(rollSeedKey, PersistentDataType.LONG);
        Byte revealed = data.get(abilityRevealedKey, PersistentDataType.BYTE);
        if (definition == null || damage == null || speed == null || maxDurability == null || rollSeed == null) {
            return Optional.empty();
        }
        return Optional.of(new DungeonWeaponData(
                definition,
                new DungeonWeaponStats(damage, speed, maxDurability),
                rollSeed,
                revealed != null && revealed == (byte) 1
        ));
    }

    public Optional<String> itemId(ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) {
            return Optional.empty();
        }
        return Optional.ofNullable(item.getItemMeta().getPersistentDataContainer()
                .get(itemIdKey, PersistentDataType.STRING));
    }

    public boolean isStarter(ItemStack item, Affinity affinity) {
        return itemId(item).filter(registry.starterFor(affinity).id()::equals).isPresent();
    }

    public ItemStack refreshedStarter(Affinity affinity) {
        return createStarter(affinity);
    }

    public boolean revealAbility(ItemStack item) {
        DungeonWeaponData weapon = read(item).orElse(null);
        if (weapon == null || weapon.definition().ability() == WeaponAbility.NONE || weapon.abilityRevealed()) {
            return false;
        }
        apply(item, new DungeonWeaponData(weapon.definition(), weapon.stats(), weapon.rollSeed(), true));
        return true;
    }

    public void refresh(ItemStack item) {
        DungeonWeaponData weapon = read(item).orElseThrow(() ->
                new IllegalArgumentException("That item is not a dungeon weapon."));
        apply(item, weapon);
    }

    public double effectiveDamage(ItemStack item, DungeonWeaponData weapon) {
        double runeBonus = DungeonRuneCodec.bonus(socketedRunes(item), DungeonRuneEffect.FORCE);
        return weapon.stats().damage() * (1.0D + runeBonus);
    }

    private void apply(ItemStack item, DungeonWeaponData weapon) {
        DungeonWeaponDefinition definition = weapon.definition();
        DungeonWeaponStats stats = weapon.stats();
        List<DungeonRuneDefinition> runes = socketedRunes(item);
        double effectiveDamage = effectiveDamage(item, weapon);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(definition.displayName(), definition.rarity().textColor())
                .decorate(TextDecoration.BOLD)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(buildLore(weapon, runes, effectiveDamage));
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_UNBREAKABLE);

        PersistentDataContainer data = meta.getPersistentDataContainer();
        data.set(itemIdKey, PersistentDataType.STRING, definition.id());
        data.set(rarityKey, PersistentDataType.STRING, definition.rarity().name());
        data.set(schemaKey, PersistentDataType.INTEGER, ITEM_SCHEMA_VERSION);
        data.set(affinityKey, PersistentDataType.STRING, definition.affinity().id());
        data.set(damageKey, PersistentDataType.DOUBLE, stats.damage());
        data.set(speedKey, PersistentDataType.DOUBLE, stats.speed());
        data.set(maxDurabilityKey, PersistentDataType.INTEGER, stats.maxDurability());
        data.set(abilityKey, PersistentDataType.STRING, definition.ability().id());
        data.set(abilityRevealedKey, PersistentDataType.BYTE, (byte) (weapon.abilityRevealed() ? 1 : 0));
        data.set(rollSeedKey, PersistentDataType.LONG, weapon.rollSeed());

        if (definition.affinity() != Affinity.ARBALIST) {
            meta.removeAttributeModifier(Attribute.ATTACK_DAMAGE);
            meta.removeAttributeModifier(Attribute.ATTACK_SPEED);
            meta.addAttributeModifier(Attribute.ATTACK_DAMAGE, new AttributeModifier(
                    attackDamageModifierKey,
                    effectiveDamage - 1.0D,
                    AttributeModifier.Operation.ADD_NUMBER,
                    EquipmentSlotGroup.MAINHAND
            ));
            meta.addAttributeModifier(Attribute.ATTACK_SPEED, new AttributeModifier(
                    attackSpeedModifierKey,
                    stats.speed() - 4.0D,
                    AttributeModifier.Operation.ADD_NUMBER,
                    EquipmentSlotGroup.MAINHAND
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
        if (definition.affinity() == Affinity.ARBALIST && definition.rarity().ordinal() >= ItemRarity.RARE.ordinal()) {
            meta.addEnchant(Enchantment.QUICK_CHARGE, 1, true);
        }
        item.setItemMeta(meta);
    }

    private List<Component> buildLore(
            DungeonWeaponData weapon,
            List<DungeonRuneDefinition> runes,
            double effectiveDamage
    ) {
        DungeonWeaponDefinition definition = weapon.definition();
        DungeonWeaponStats stats = weapon.stats();
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text(definition.rarity().displayName(), definition.rarity().textColor())
                .decorate(TextDecoration.BOLD)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        lore.add(statLine("Affinity", definition.affinity().displayName(), NamedTextColor.AQUA));
        lore.add(statLine("Damage", String.format(Locale.ROOT, "%.1f", effectiveDamage), NamedTextColor.RED));
        lore.add(statLine("Speed", String.format(Locale.ROOT, "%.2f", stats.speed()), NamedTextColor.YELLOW));
        lore.add(statLine(
                "Durability",
                stats.maxDurability() == 0 ? "Infinite" : Integer.toString(stats.maxDurability()),
                NamedTextColor.GREEN
        ));

        if (weapon.abilityRevealed() && definition.ability() != WeaponAbility.NONE) {
            lore.add(Component.empty());
            lore.add(Component.text("Ability: ", NamedTextColor.DARK_PURPLE)
                    .append(Component.text(definition.ability().displayName(), NamedTextColor.LIGHT_PURPLE))
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text(definition.ability().description(), NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false));
        }
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
