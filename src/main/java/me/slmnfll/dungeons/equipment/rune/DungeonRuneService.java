package me.slmnfll.dungeons.equipment.rune;

import me.slmnfll.dungeons.content.DungeonItemDeliveryService;
import me.slmnfll.dungeons.equipment.DungeonItemCategory;
import me.slmnfll.dungeons.equipment.armor.DungeonArmorService;
import me.slmnfll.dungeons.equipment.weapon.DungeonWeaponService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;

public final class DungeonRuneService {
    public static final int ITEM_SCHEMA_VERSION = 1;

    private final DungeonRuneRegistry registry;
    private final DungeonWeaponService weaponService;
    private final DungeonArmorService armorService;
    private final DungeonItemDeliveryService itemDeliveryService;
    private final NamespacedKey itemIdKey;
    private final NamespacedKey rarityKey;
    private final NamespacedKey schemaKey;
    private final NamespacedKey categoryKey;
    private final NamespacedKey runeEffectKey;
    private final NamespacedKey runeTargetKey;
    private final NamespacedKey socketedRunesKey;

    public DungeonRuneService(
            JavaPlugin plugin,
            DungeonRuneRegistry registry,
            DungeonWeaponService weaponService,
            DungeonArmorService armorService,
            DungeonItemDeliveryService itemDeliveryService
    ) {
        this.registry = Objects.requireNonNull(registry, "registry");
        this.weaponService = Objects.requireNonNull(weaponService, "weaponService");
        this.armorService = Objects.requireNonNull(armorService, "armorService");
        this.itemDeliveryService = Objects.requireNonNull(itemDeliveryService, "itemDeliveryService");
        this.itemIdKey = new NamespacedKey(plugin, "item_id");
        this.rarityKey = new NamespacedKey(plugin, "rarity");
        this.schemaKey = new NamespacedKey(plugin, "item_schema");
        this.categoryKey = new NamespacedKey(plugin, "item_category");
        this.runeEffectKey = new NamespacedKey(plugin, "rune_effect");
        this.runeTargetKey = new NamespacedKey(plugin, "rune_target");
        this.socketedRunesKey = new NamespacedKey(plugin, "socketed_runes");
    }

    public ItemStack create(String runeId) {
        DungeonRuneDefinition definition = registry.find(runeId).orElseThrow(() ->
                new IllegalArgumentException("Unknown rune. Use force, ward, restoration, cinders, or rift."));
        return create(definition);
    }

    public ItemStack createLootRune(Random random) {
        return create(registry.rollLoot(random));
    }

    public Optional<DungeonRuneDefinition> readRune(ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) {
            return Optional.empty();
        }
        String itemId = item.getItemMeta().getPersistentDataContainer()
                .get(itemIdKey, PersistentDataType.STRING);
        return itemId == null ? Optional.empty() : registry.find(itemId);
    }

    public DungeonRuneDefinition socket(Player player) {
        ItemStack equipment = player.getInventory().getItemInMainHand();
        ItemStack runeItem = player.getInventory().getItemInOffHand();
        DungeonRuneDefinition rune = socket(equipment, runeItem);
        consumeOffHand(player);
        return rune;
    }

    public DungeonRuneDefinition socket(ItemStack equipment, ItemStack runeItem) {
        DungeonRuneDefinition rune = readRune(runeItem).orElseThrow(() ->
                new IllegalStateException("Place a socketable rune in the rune slot."));
        if (!rune.effect().socketable()) {
            throw new IllegalStateException("The Rune of Restoration repairs equipment instead of occupying a socket.");
        }

        boolean weapon = weaponService.read(equipment).isPresent();
        boolean armor = armorService.read(equipment).isPresent();
        if (!weapon && !armor) {
            throw new IllegalStateException("Place a dungeon weapon or armor piece in the equipment slot.");
        }
        if (!rune.target().supports(weapon)) {
            throw new IllegalStateException(rune.displayName() + " can only be applied to "
                    + rune.target().displayName().toLowerCase() + ".");
        }

        List<DungeonRuneDefinition> runes = new ArrayList<>(socketedRunes(equipment));
        if (runes.size() >= DungeonRuneCodec.MAX_SOCKETS) {
            throw new IllegalStateException("That equipment already has all three rune sockets filled.");
        }
        runes.add(rune);
        writeSockets(equipment, runes);
        refreshEquipment(equipment, weapon);
        return rune;
    }

    public DungeonRuneDefinition unsocket(Player player, int oneBasedSlot) {
        ItemStack equipment = player.getInventory().getItemInMainHand();
        DungeonRuneDefinition removed = unsocket(equipment, oneBasedSlot);
        itemDeliveryService.give(player, List.of(create(removed)));
        return removed;
    }

    public DungeonRuneDefinition unsocket(ItemStack equipment, int oneBasedSlot) {
        boolean weapon = weaponService.read(equipment).isPresent();
        boolean armor = armorService.read(equipment).isPresent();
        if (!weapon && !armor) {
            throw new IllegalStateException("Place socketed dungeon equipment in the equipment slot.");
        }

        List<DungeonRuneDefinition> runes = new ArrayList<>(socketedRunes(equipment));
        if (runes.isEmpty()) {
            throw new IllegalStateException("That equipment has no socketed runes.");
        }
        if (oneBasedSlot < 1 || oneBasedSlot > runes.size()) {
            throw new IllegalArgumentException("Choose an occupied rune slot between 1 and " + runes.size() + ".");
        }
        DungeonRuneDefinition removed = runes.remove(oneBasedSlot - 1);
        writeSockets(equipment, runes);
        refreshEquipment(equipment, weapon);
        return removed;
    }

    public void repair(Player player) {
        ItemStack equipment = player.getInventory().getItemInMainHand();
        ItemStack runeItem = player.getInventory().getItemInOffHand();
        repair(equipment, runeItem);
        consumeOffHand(player);
    }

    public void repair(ItemStack equipment, ItemStack runeItem) {
        DungeonRuneDefinition rune = readRune(runeItem).orElseThrow(() ->
                new IllegalStateException("Place a Rune of Restoration in the rune slot."));
        if (rune.effect() != DungeonRuneEffect.RESTORATION) {
            throw new IllegalStateException("Only a Rune of Restoration can perform this repair.");
        }
        if (weaponService.read(equipment).isEmpty() && armorService.read(equipment).isEmpty()) {
            throw new IllegalStateException("Place damaged dungeon equipment in the equipment slot.");
        }
        ItemMeta meta = equipment.getItemMeta();
        if (!(meta instanceof Damageable damageable) || damageable.isUnbreakable()) {
            throw new IllegalStateException("That equipment does not require repairs.");
        }
        if (damageable.getDamage() <= 0) {
            throw new IllegalStateException("That equipment is already fully repaired.");
        }
        damageable.setDamage(0);
        equipment.setItemMeta(meta);
    }

    public boolean isDungeonEquipment(ItemStack item) {
        return weaponService.read(item).isPresent() || armorService.read(item).isPresent();
    }

    public List<DungeonRuneDefinition> socketedRunes(ItemStack equipment) {
        if (equipment == null || equipment.getType().isAir() || !equipment.hasItemMeta()) {
            return List.of();
        }
        String encoded = equipment.getItemMeta().getPersistentDataContainer()
                .get(socketedRunesKey, PersistentDataType.STRING);
        return DungeonRuneCodec.decode(encoded, registry);
    }

    private ItemStack create(DungeonRuneDefinition definition) {
        ItemStack item = new ItemStack(definition.material());
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(definition.displayName(), definition.rarity().textColor())
                .decorate(TextDecoration.BOLD)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(
                Component.text(definition.rarity().displayName(), definition.rarity().textColor())
                        .decorate(TextDecoration.BOLD)
                        .decoration(TextDecoration.ITALIC, false),
                Component.text("Rune", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false),
                Component.empty(),
                statLine("Target", definition.target().displayName(), NamedTextColor.AQUA),
                Component.text(definition.description(), NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false),
                Component.text(
                        definition.effect().socketable()
                                ? "Applied and removed by a Rune-smith"
                                : "Consumed when used by a Rune-smith",
                        NamedTextColor.DARK_GRAY
                ).decoration(TextDecoration.ITALIC, false)
        ));
        PersistentDataContainer data = meta.getPersistentDataContainer();
        data.set(itemIdKey, PersistentDataType.STRING, definition.id());
        data.set(rarityKey, PersistentDataType.STRING, definition.rarity().name());
        data.set(schemaKey, PersistentDataType.INTEGER, ITEM_SCHEMA_VERSION);
        data.set(categoryKey, PersistentDataType.STRING, DungeonItemCategory.RUNE.id());
        data.set(runeEffectKey, PersistentDataType.STRING, definition.effect().name());
        data.set(runeTargetKey, PersistentDataType.STRING, definition.target().name());
        item.setItemMeta(meta);
        return item;
    }

    private void writeSockets(ItemStack equipment, List<DungeonRuneDefinition> runes) {
        ItemMeta meta = equipment.getItemMeta();
        if (runes.isEmpty()) {
            meta.getPersistentDataContainer().remove(socketedRunesKey);
        } else {
            meta.getPersistentDataContainer().set(
                    socketedRunesKey,
                    PersistentDataType.STRING,
                    DungeonRuneCodec.encode(runes)
            );
        }
        equipment.setItemMeta(meta);
    }

    private void refreshEquipment(ItemStack equipment, boolean weapon) {
        if (weapon) {
            weaponService.refresh(equipment);
        } else {
            armorService.refresh(equipment);
        }
    }

    private void consumeOffHand(Player player) {
        ItemStack rune = player.getInventory().getItemInOffHand();
        if (rune.getAmount() <= 1) {
            player.getInventory().setItemInOffHand(null);
        } else {
            rune.setAmount(rune.getAmount() - 1);
        }
    }

    private Component statLine(String label, String value, NamedTextColor valueColor) {
        return Component.text(label + ": ", NamedTextColor.GRAY)
                .append(Component.text(value, valueColor))
                .decoration(TextDecoration.ITALIC, false);
    }
}
