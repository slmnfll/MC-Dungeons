package me.slmnfll.dungeons.equipment.rune.smith;

import me.slmnfll.dungeons.equipment.rune.DungeonRuneCodec;
import me.slmnfll.dungeons.equipment.rune.DungeonRuneDefinition;
import me.slmnfll.dungeons.equipment.rune.DungeonRuneService;
import me.slmnfll.dungeons.ui.DungeonGuiItems;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

final class DungeonRuneSmithMenu implements InventoryHolder {
    private final UUID ownerId;
    private final Inventory inventory;
    private boolean returned;

    DungeonRuneSmithMenu(UUID ownerId) {
        this.ownerId = ownerId;
        this.inventory = Bukkit.createInventory(
                this,
                DungeonRuneSmithLayout.SIZE,
                Component.text("Rune-smith", NamedTextColor.DARK_PURPLE)
        );
        buildFrame();
    }

    UUID ownerId() {
        return ownerId;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }

    ItemStack equipment() {
        return inventory.getItem(DungeonRuneSmithLayout.EQUIPMENT_INPUT_SLOT);
    }

    ItemStack rune() {
        return inventory.getItem(DungeonRuneSmithLayout.RUNE_INPUT_SLOT);
    }

    boolean hasItem(int slot) {
        ItemStack item = inventory.getItem(slot);
        return item != null && !item.getType().isAir();
    }

    void setInput(int slot, ItemStack item) {
        inventory.setItem(slot, item);
    }

    void consumeRune() {
        ItemStack rune = rune();
        if (rune == null || rune.getType().isAir()) {
            return;
        }
        if (rune.getAmount() <= 1) {
            inventory.setItem(DungeonRuneSmithLayout.RUNE_INPUT_SLOT, null);
        } else {
            rune.setAmount(rune.getAmount() - 1);
            inventory.setItem(DungeonRuneSmithLayout.RUNE_INPUT_SLOT, rune);
        }
    }

    List<ItemStack> drainInputs() {
        if (returned) {
            return List.of();
        }
        returned = true;
        List<ItemStack> items = new ArrayList<>();
        for (int slot : List.of(
                DungeonRuneSmithLayout.EQUIPMENT_INPUT_SLOT,
                DungeonRuneSmithLayout.RUNE_INPUT_SLOT
        )) {
            ItemStack item = inventory.getItem(slot);
            if (item != null && !item.getType().isAir()) {
                items.add(item);
                inventory.setItem(slot, null);
            }
        }
        return items;
    }

    void refresh(DungeonRuneService runeService) {
        List<DungeonRuneDefinition> runes = runeService.socketedRunes(equipment());
        setStatus(
                runeService.isDungeonEquipment(equipment())
                        ? runes.size() + "/" + DungeonRuneCodec.MAX_SOCKETS + " rune sockets occupied"
                        : "Place dungeon equipment and a rune below",
                runeService.isDungeonEquipment(equipment()) ? NamedTextColor.AQUA : NamedTextColor.GRAY
        );

        for (int index = 0; index < DungeonRuneSmithLayout.SOCKET_DISPLAY_SLOTS.size(); index++) {
            int displaySlot = DungeonRuneSmithLayout.SOCKET_DISPLAY_SLOTS.get(index);
            if (index >= runes.size()) {
                inventory.setItem(displaySlot, item(
                        Material.GRAY_DYE,
                        "Empty Rune Socket " + (index + 1),
                        NamedTextColor.DARK_GRAY,
                        List.of("Socket a rune to fill this slot")
                ));
                continue;
            }
            DungeonRuneDefinition rune = runes.get(index);
            ItemStack display = runeService.create(rune.id());
            ItemMeta meta = display.getItemMeta();
            List<Component> lore = meta.lore() == null
                    ? new ArrayList<>()
                    : new ArrayList<>(meta.lore());
            lore.add(Component.empty());
            lore.add(Component.text("Click to remove and preserve this rune", NamedTextColor.YELLOW)
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(lore);
            display.setItemMeta(meta);
            inventory.setItem(displaySlot, display);
        }
    }

    void setStatus(String message, NamedTextColor color) {
        inventory.setItem(DungeonRuneSmithLayout.INFO_SLOT, item(
                Material.ENCHANTED_BOOK,
                "Rune-smith Workshop",
                color,
                List.of(message, "Your deposited items are returned when this menu closes")
        ));
    }

    private void buildFrame() {
        ItemStack filler = DungeonGuiItems.tooltipFreeGlassPane(Material.BLACK_STAINED_GLASS_PANE);
        for (int slot = 0; slot < DungeonRuneSmithLayout.SIZE; slot++) {
            inventory.setItem(slot, filler);
        }
        inventory.setItem(DungeonRuneSmithLayout.EQUIPMENT_INPUT_SLOT, null);
        inventory.setItem(DungeonRuneSmithLayout.RUNE_INPUT_SLOT, null);
        inventory.setItem(DungeonRuneSmithLayout.EQUIPMENT_LABEL_SLOT, item(
                Material.DIAMOND_CHESTPLATE,
                "Equipment Input",
                NamedTextColor.AQUA,
                List.of("Place one dungeon weapon or armor piece below")
        ));
        inventory.setItem(DungeonRuneSmithLayout.RUNE_LABEL_SLOT, item(
                Material.ECHO_SHARD,
                "Rune Input",
                NamedTextColor.LIGHT_PURPLE,
                List.of("Place a dungeon rune below")
        ));
        inventory.setItem(DungeonRuneSmithLayout.SOCKET_ACTION_SLOT, item(
                Material.ANVIL,
                "Socket Rune",
                NamedTextColor.GREEN,
                List.of("Consumes one socketable rune", "Duplicate runes are allowed")
        ));
        inventory.setItem(DungeonRuneSmithLayout.REPAIR_ACTION_SLOT, item(
                Material.GRINDSTONE,
                "Restore Equipment",
                NamedTextColor.GOLD,
                List.of("Consumes one Rune of Restoration")
        ));
        inventory.setItem(DungeonRuneSmithLayout.CLOSE_SLOT, item(
                Material.BARRIER,
                "Close Workshop",
                NamedTextColor.RED,
                List.of("Returns all deposited items")
        ));
    }

    private ItemStack item(Material material, String name, NamedTextColor color, List<String> loreLines) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name, color)
                .decorate(TextDecoration.BOLD)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(loreLines.stream()
                .map(line -> Component.text(line, NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false))
                .toList());
        item.setItemMeta(meta);
        return item;
    }
}
