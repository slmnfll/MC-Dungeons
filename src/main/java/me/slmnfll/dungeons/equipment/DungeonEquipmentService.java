package me.slmnfll.dungeons.equipment;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

public final class DungeonEquipmentService {
    public static final String GUILD_STARTER_KEY = "guild_starter_item";
    private static final int MAX_REPORTED_ITEMS = 4;

    private final DungeonCarryLimitPolicy carryLimitPolicy = new DungeonCarryLimitPolicy();
    private final NamespacedKey itemIdKey;
    private final NamespacedKey categoryKey;
    private final NamespacedKey guildStarterKey;

    public DungeonEquipmentService(JavaPlugin plugin) {
        this.itemIdKey = new NamespacedKey(plugin, "item_id");
        this.categoryKey = new NamespacedKey(plugin, "item_category");
        this.guildStarterKey = new NamespacedKey(plugin, GUILD_STARTER_KEY);
    }

    public void validateEntry(Player player) {
        Set<String> disallowed = new LinkedHashSet<>();
        int limitedItems = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null || item.getType().isAir()) {
                continue;
            }
            if (countsTowardCarryLimit(item)) {
                limitedItems += item.getAmount();
            }
            if (isDungeonItem(item)) {
                continue;
            }
            if (DungeonEquipmentPolicy.isRestrictedVanillaEquipment(item.getType())) {
                disallowed.add(readableName(item.getType()));
            }
        }
        if (!disallowed.isEmpty()) {
            String summary = disallowed.stream()
                    .limit(MAX_REPORTED_ITEMS)
                    .reduce((left, right) -> left + ", " + right)
                    .orElse("");
            if (disallowed.size() > MAX_REPORTED_ITEMS) {
                summary += ", and " + (disallowed.size() - MAX_REPORTED_ITEMS) + " more";
            }
            throw new IllegalStateException(
                    "Vanilla equipment cannot enter a dungeon. Remove: " + summary
                            + ". Dungeons-tagged equipment is allowed."
            );
        }
        if (!carryLimitPolicy.allows(limitedItems)) {
            throw new IllegalStateException(
                    "You can carry at most " + DungeonCarryLimitPolicy.MAX_ARTIFACTS_AND_CONSUMABLES
                            + " artifacts and dungeon consumables. Remove "
                            + carryLimitPolicy.excess(limitedItems) + " item(s)."
            );
        }
    }

    public void validateTutorialEntry(Player player) {
        int carriedItems = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null || item.getType().isAir() || isGuildStarterItem(item)) {
                continue;
            }
            carriedItems += item.getAmount();
        }
        if (carriedItems > 0) {
            throw new IllegalStateException(
                    "Survey Site Zero requires an empty inventory. Store every carried item, including armor and offhand items, before beginning."
            );
        }
    }

    public boolean isDungeonItem(ItemStack item) {
        return item != null
                && item.hasItemMeta()
                && item.getItemMeta().getPersistentDataContainer().has(itemIdKey, PersistentDataType.STRING);
    }

    private boolean countsTowardCarryLimit(ItemStack item) {
        if (!item.hasItemMeta()) {
            return false;
        }
        String category = item.getItemMeta().getPersistentDataContainer()
                .get(categoryKey, PersistentDataType.STRING);
        return DungeonItemCategory.ARTIFACT.id().equals(category)
                || DungeonItemCategory.CONSUMABLE.id().equals(category);
    }

    private boolean isGuildStarterItem(ItemStack item) {
        return item.hasItemMeta()
                && item.getItemMeta().getPersistentDataContainer()
                .has(guildStarterKey, PersistentDataType.BYTE);
    }

    private String readableName(Material material) {
        String[] words = material.name().toLowerCase(Locale.ROOT).split("_");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (!result.isEmpty()) {
                result.append(' ');
            }
            result.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return result.toString();
    }
}
