package me.slmnfll.dungeons.equipment.consumable;

import me.slmnfll.dungeons.equipment.DungeonItemCategory;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.Locale;

public final class DungeonConsumableService {
    public static final int ITEM_SCHEMA_VERSION = 1;

    private final DungeonConsumableRegistry registry;
    private final NamespacedKey itemIdKey;
    private final NamespacedKey rarityKey;
    private final NamespacedKey schemaKey;
    private final NamespacedKey categoryKey;
    private final NamespacedKey abilityKey;

    public DungeonConsumableService(JavaPlugin plugin, DungeonConsumableRegistry registry) {
        this.registry = Objects.requireNonNull(registry, "registry");
        this.itemIdKey = new NamespacedKey(plugin, "item_id");
        this.rarityKey = new NamespacedKey(plugin, "rarity");
        this.schemaKey = new NamespacedKey(plugin, "item_schema");
        this.categoryKey = new NamespacedKey(plugin, "item_category");
        this.abilityKey = new NamespacedKey(plugin, "consumable_ability");
    }

    public ItemStack create(String consumableId) {
        DungeonConsumableDefinition definition = registry.find(consumableId).orElseThrow(() ->
                new IllegalArgumentException(
                        "Unknown consumable. Use mossbread, sunroot, deepdelve_stew, lumen_berries, healing, stonehide, or cinder."
                ));
        return create(definition);
    }

    public ItemStack createLootConsumable(Random random) {
        return create(registry.rollLoot(random));
    }

    public Optional<DungeonConsumableDefinition> read(ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) {
            return Optional.empty();
        }
        String itemId = item.getItemMeta().getPersistentDataContainer()
                .get(itemIdKey, PersistentDataType.STRING);
        return itemId == null ? Optional.empty() : registry.find(itemId);
    }

    private ItemStack create(DungeonConsumableDefinition definition) {
        ItemStack item = new ItemStack(definition.material());
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(definition.displayName(), definition.rarity().textColor())
                .decorate(TextDecoration.BOLD)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(buildLore(definition));
        if (meta instanceof PotionMeta potionMeta && definition.color() != null) {
            potionMeta.setColor(definition.color());
        }

        PersistentDataContainer data = meta.getPersistentDataContainer();
        data.set(itemIdKey, PersistentDataType.STRING, definition.id());
        data.set(rarityKey, PersistentDataType.STRING, definition.rarity().name());
        data.set(schemaKey, PersistentDataType.INTEGER, ITEM_SCHEMA_VERSION);
        data.set(categoryKey, PersistentDataType.STRING, DungeonItemCategory.CONSUMABLE.id());
        data.set(abilityKey, PersistentDataType.STRING, definition.ability().name());
        item.setItemMeta(meta);
        return item;
    }

    private List<Component> buildLore(DungeonConsumableDefinition definition) {
        java.util.ArrayList<Component> lore = new java.util.ArrayList<>();
        lore.add(Component.text(definition.rarity().displayName(), definition.rarity().textColor())
                .decorate(TextDecoration.BOLD)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Dungeon Consumable", NamedTextColor.DARK_GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        lore.add(Component.text("Effect: ", NamedTextColor.GRAY)
                .append(Component.text(definition.ability().displayName(), NamedTextColor.AQUA))
                .decoration(TextDecoration.ITALIC, false));
        if (definition.regenerationLevel() > 0) {
            lore.add(statLine("Regeneration", romanNumeral(definition.regenerationLevel())));
        }
        if (definition.durationTicks() > 0) {
            lore.add(statLine("Duration", seconds(definition.durationTicks())));
        }
        lore.add(statLine("Cooldown", seconds(definition.cooldownTicks())));
        lore.add(Component.text(definition.ability().description(), NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text(
                definition.throwable() ? "Throw to use" : definition.food() ? "Right-click to consume" : "Drink to use",
                NamedTextColor.DARK_GRAY
        ).decoration(TextDecoration.ITALIC, false));
        return List.copyOf(lore);
    }

    private Component statLine(String name, String value) {
        return Component.text(name + ": ", NamedTextColor.GRAY)
                .append(Component.text(value, NamedTextColor.YELLOW))
                .decoration(TextDecoration.ITALIC, false);
    }

    private String seconds(int ticks) {
        return String.format(Locale.ROOT, "%.1fs", ticks / 20.0D);
    }

    private String romanNumeral(int level) {
        return switch (level) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            default -> Integer.toString(level);
        };
    }
}
