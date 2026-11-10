package me.slmnfll.dungeons.equipment.artifact;

import me.slmnfll.dungeons.affinity.Affinity;
import me.slmnfll.dungeons.equipment.DungeonItemCategory;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

public final class DungeonArtifactService {
    public static final int ITEM_SCHEMA_VERSION = 1;

    private final DungeonArtifactRegistry registry;
    private final NamespacedKey itemIdKey;
    private final NamespacedKey rarityKey;
    private final NamespacedKey schemaKey;
    private final NamespacedKey categoryKey;
    private final NamespacedKey affinityKey;
    private final NamespacedKey abilityKey;
    private final NamespacedKey cooldownKey;
    private final NamespacedKey abilityRevealedKey;

    public DungeonArtifactService(JavaPlugin plugin, DungeonArtifactRegistry registry) {
        this.registry = Objects.requireNonNull(registry, "registry");
        this.itemIdKey = new NamespacedKey(plugin, "item_id");
        this.rarityKey = new NamespacedKey(plugin, "rarity");
        this.schemaKey = new NamespacedKey(plugin, "item_schema");
        this.categoryKey = new NamespacedKey(plugin, "item_category");
        this.affinityKey = new NamespacedKey(plugin, "artifact_affinity");
        this.abilityKey = new NamespacedKey(plugin, "artifact_ability");
        this.cooldownKey = new NamespacedKey(plugin, "artifact_cooldown");
        this.abilityRevealedKey = new NamespacedKey(plugin, "ability_revealed");
    }

    public ItemStack createLootArtifact(Affinity affinity) {
        return create(registry.firstFor(affinity), false);
    }

    public ItemStack createStarterArtifact(Affinity affinity) {
        return create(registry.firstFor(affinity), true);
    }

    public ItemStack create(DungeonArtifactDefinition definition, boolean abilityRevealed) {
        ItemStack item = new ItemStack(definition.material());
        apply(item, new DungeonArtifactData(definition, abilityRevealed));
        return item;
    }

    public Optional<DungeonArtifactData> read(ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) {
            return Optional.empty();
        }
        PersistentDataContainer data = item.getItemMeta().getPersistentDataContainer();
        String itemId = data.get(itemIdKey, PersistentDataType.STRING);
        DungeonArtifactDefinition definition = itemId == null ? null : registry.find(itemId).orElse(null);
        Byte revealed = data.get(abilityRevealedKey, PersistentDataType.BYTE);
        if (definition == null) {
            return Optional.empty();
        }
        return Optional.of(new DungeonArtifactData(definition, revealed != null && revealed == (byte) 1));
    }

    public boolean revealAbility(ItemStack item) {
        DungeonArtifactData artifact = read(item).orElse(null);
        if (artifact == null || artifact.abilityRevealed()) {
            return false;
        }
        apply(item, new DungeonArtifactData(artifact.definition(), true));
        return true;
    }

    private void apply(ItemStack item, DungeonArtifactData artifact) {
        DungeonArtifactDefinition definition = artifact.definition();
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(definition.displayName(), definition.rarity().textColor())
                .decorate(TextDecoration.BOLD)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(buildLore(artifact));
        meta.setMaxStackSize(1);
        meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

        PersistentDataContainer data = meta.getPersistentDataContainer();
        data.set(itemIdKey, PersistentDataType.STRING, definition.id());
        data.set(rarityKey, PersistentDataType.STRING, definition.rarity().name());
        data.set(schemaKey, PersistentDataType.INTEGER, ITEM_SCHEMA_VERSION);
        data.set(categoryKey, PersistentDataType.STRING, DungeonItemCategory.ARTIFACT.id());
        data.set(affinityKey, PersistentDataType.STRING, definition.affinity().id());
        data.set(abilityKey, PersistentDataType.STRING, definition.ability().id());
        data.set(cooldownKey, PersistentDataType.INTEGER, definition.cooldownTicks());
        data.set(abilityRevealedKey, PersistentDataType.BYTE, (byte) (artifact.abilityRevealed() ? 1 : 0));
        item.setItemMeta(meta);
    }

    private List<Component> buildLore(DungeonArtifactData artifact) {
        DungeonArtifactDefinition definition = artifact.definition();
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text(definition.rarity().displayName(), definition.rarity().textColor())
                .decorate(TextDecoration.BOLD)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Artifact", NamedTextColor.DARK_GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        lore.add(statLine("Affinity", definition.affinity().displayName(), NamedTextColor.AQUA));
        lore.add(statLine(
                "Cooldown",
                String.format(Locale.ROOT, "%.1fs", definition.cooldownTicks() / 20.0D),
                NamedTextColor.YELLOW
        ));
        lore.add(Component.empty());
        if (artifact.abilityRevealed()) {
            lore.add(Component.text("Ability: ", NamedTextColor.DARK_PURPLE)
                    .append(Component.text(definition.ability().displayName(), NamedTextColor.LIGHT_PURPLE))
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text(definition.ability().description(), NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false));
        } else {
            lore.add(Component.text("Ability: Unidentified", NamedTextColor.DARK_PURPLE)
                    .decoration(TextDecoration.ITALIC, false));
        }
        lore.add(Component.text("Right-click to activate", NamedTextColor.DARK_GRAY)
                .decoration(TextDecoration.ITALIC, false));
        return List.copyOf(lore);
    }

    private Component statLine(String label, String value, NamedTextColor valueColor) {
        return Component.text(label + ": ", NamedTextColor.GRAY)
                .append(Component.text(value, valueColor))
                .decoration(TextDecoration.ITALIC, false);
    }
}
