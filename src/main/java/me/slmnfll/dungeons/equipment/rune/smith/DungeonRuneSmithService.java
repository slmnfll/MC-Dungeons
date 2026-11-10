package me.slmnfll.dungeons.equipment.rune.smith;

import me.slmnfll.dungeons.content.DungeonItemDeliveryService;
import me.slmnfll.dungeons.equipment.rune.DungeonRuneDefinition;
import me.slmnfll.dungeons.equipment.rune.DungeonRuneService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class DungeonRuneSmithService implements Listener {
    private final JavaPlugin plugin;
    private final DungeonRuneService runeService;
    private final DungeonItemDeliveryService itemDeliveryService;
    private final Map<UUID, DungeonRuneSmithMenu> activeMenus = new HashMap<>();

    public DungeonRuneSmithService(
            JavaPlugin plugin,
            DungeonRuneService runeService,
            DungeonItemDeliveryService itemDeliveryService
    ) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.runeService = Objects.requireNonNull(runeService, "runeService");
        this.itemDeliveryService = Objects.requireNonNull(itemDeliveryService, "itemDeliveryService");
    }

    public void open(Player player) {
        DungeonRuneSmithMenu previous = activeMenus.remove(player.getUniqueId());
        if (previous != null) {
            returnInputs(player, previous);
        }
        player.closeInventory();

        DungeonRuneSmithMenu menu = new DungeonRuneSmithMenu(player.getUniqueId());
        activeMenus.put(player.getUniqueId(), menu);
        menu.refresh(runeService);
        player.openInventory(menu.getInventory());
        player.playSound(player.getLocation(), Sound.BLOCK_SMITHING_TABLE_USE, 0.8F, 1.0F);
    }

    public void shutdown() {
        for (Map.Entry<UUID, DungeonRuneSmithMenu> entry : List.copyOf(activeMenus.entrySet())) {
            Player player = plugin.getServer().getPlayer(entry.getKey());
            if (player != null) {
                returnInputs(player, entry.getValue());
                player.closeInventory();
            }
        }
        activeMenus.clear();
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof DungeonRuneSmithMenu menu)) {
            return;
        }
        if (!(event.getWhoClicked() instanceof Player player) || !menu.ownerId().equals(player.getUniqueId())) {
            event.setCancelled(true);
            return;
        }

        int rawSlot = event.getRawSlot();
        if (rawSlot < 0) {
            return;
        }
        if (rawSlot >= DungeonRuneSmithLayout.SIZE) {
            handlePlayerInventoryClick(event, player, menu);
            return;
        }

        if (DungeonRuneSmithLayout.isInputSlot(rawSlot)) {
            if (event.isShiftClick()) {
                event.setCancelled(true);
                ItemStack item = event.getCurrentItem();
                if (item != null && !item.getType().isAir()) {
                    menu.setInput(rawSlot, null);
                    itemDeliveryService.give(player, List.of(item));
                    refreshLater(player, menu);
                }
                return;
            }
            if (!acceptsClickInput(event, player, rawSlot)) {
                event.setCancelled(true);
                rejectInput(player, menu, rawSlot);
                return;
            }
            event.setCancelled(false);
            refreshLater(player, menu);
            return;
        }

        event.setCancelled(true);
        if (rawSlot == DungeonRuneSmithLayout.SOCKET_ACTION_SLOT) {
            socket(player, menu);
        } else if (rawSlot == DungeonRuneSmithLayout.REPAIR_ACTION_SLOT) {
            repair(player, menu);
        } else if (rawSlot == DungeonRuneSmithLayout.CLOSE_SLOT) {
            player.closeInventory();
        } else {
            int runeSlot = DungeonRuneSmithLayout.oneBasedRuneSlot(rawSlot);
            if (runeSlot > 0) {
                unsocket(player, menu, runeSlot);
            }
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof DungeonRuneSmithMenu menu)) {
            return;
        }
        boolean touchesBlockedSlot = event.getRawSlots().stream()
                .filter(slot -> slot < DungeonRuneSmithLayout.SIZE)
                .anyMatch(slot -> !DungeonRuneSmithLayout.isInputSlot(slot));
        if (touchesBlockedSlot) {
            event.setCancelled(true);
            return;
        }
        for (Map.Entry<Integer, ItemStack> entry : event.getNewItems().entrySet()) {
            int rawSlot = entry.getKey();
            if (rawSlot >= DungeonRuneSmithLayout.SIZE
                    || !DungeonRuneSmithLayout.isInputSlot(rawSlot)) {
                continue;
            }
            if (!acceptsInput(rawSlot, entry.getValue())) {
                event.setCancelled(true);
                if (event.getWhoClicked() instanceof Player player) {
                    rejectInput(player, menu, rawSlot);
                }
                return;
            }
        }
        if (event.getRawSlots().stream().anyMatch(slot -> slot < DungeonRuneSmithLayout.SIZE)
                && event.getWhoClicked() instanceof Player player) {
            refreshLater(player, menu);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof DungeonRuneSmithMenu menu)
                || !(event.getPlayer() instanceof Player player)) {
            return;
        }
        activeMenus.remove(menu.ownerId(), menu);
        returnInputs(player, menu);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        DungeonRuneSmithMenu menu = activeMenus.remove(event.getPlayer().getUniqueId());
        if (menu != null) {
            returnInputs(event.getPlayer(), menu);
        }
    }

    private void handlePlayerInventoryClick(
            InventoryClickEvent event,
            Player player,
            DungeonRuneSmithMenu menu
    ) {
        if (event.getClick() == ClickType.DOUBLE_CLICK) {
            event.setCancelled(true);
            return;
        }
        if (!event.isShiftClick()) {
            event.setCancelled(false);
            return;
        }

        event.setCancelled(true);
        ItemStack item = event.getCurrentItem();
        if (item == null || item.getType().isAir()) {
            return;
        }
        int destination;
        if (runeService.readRune(item).isPresent()) {
            destination = DungeonRuneSmithLayout.RUNE_INPUT_SLOT;
        } else if (runeService.isDungeonEquipment(item)) {
            destination = DungeonRuneSmithLayout.EQUIPMENT_INPUT_SLOT;
        } else {
            feedback(player, menu, "Only dungeon equipment and runes belong in this workshop.", false);
            return;
        }
        if (menu.hasItem(destination)) {
            feedback(player, menu, "That input slot is already occupied.", false);
            return;
        }
        menu.setInput(destination, item);
        event.setCurrentItem(null);
        menu.refresh(runeService);
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.6F, 1.15F);
    }

    private boolean acceptsClickInput(InventoryClickEvent event, Player player, int inputSlot) {
        InventoryAction action = event.getAction();
        if (action == InventoryAction.CLONE_STACK) {
            return false;
        }

        ItemStack incoming = switch (action) {
            case PLACE_ALL, PLACE_ONE, PLACE_SOME, SWAP_WITH_CURSOR -> event.getCursor();
            case HOTBAR_SWAP, HOTBAR_MOVE_AND_READD -> hotbarSwapItem(event, player);
            default -> null;
        };
        return incoming == null || incoming.getType().isAir() || acceptsInput(inputSlot, incoming);
    }

    private ItemStack hotbarSwapItem(InventoryClickEvent event, Player player) {
        if (event.getClick() == ClickType.SWAP_OFFHAND) {
            return player.getInventory().getItemInOffHand();
        }
        int hotbarButton = event.getHotbarButton();
        return hotbarButton >= 0 && hotbarButton < 9
                ? player.getInventory().getItem(hotbarButton)
                : null;
    }

    private boolean acceptsInput(int inputSlot, ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return true;
        }
        if (inputSlot == DungeonRuneSmithLayout.EQUIPMENT_INPUT_SLOT) {
            return runeService.isDungeonEquipment(item);
        }
        if (inputSlot == DungeonRuneSmithLayout.RUNE_INPUT_SLOT) {
            return runeService.readRune(item).isPresent();
        }
        return false;
    }

    private void rejectInput(Player player, DungeonRuneSmithMenu menu, int inputSlot) {
        String message = inputSlot == DungeonRuneSmithLayout.EQUIPMENT_INPUT_SLOT
                ? "Only a dungeon weapon or armor piece fits in the equipment slot."
                : "Only a dungeon rune fits in the rune slot.";
        feedback(player, menu, message, false);
    }

    private void socket(Player player, DungeonRuneSmithMenu menu) {
        try {
            DungeonRuneDefinition rune = runeService.socket(menu.equipment(), menu.rune());
            menu.consumeRune();
            menu.refresh(runeService);
            feedback(player, menu, rune.displayName() + " was socketed.", true);
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 0.8F, 1.25F);
        } catch (IllegalArgumentException | IllegalStateException exception) {
            feedback(player, menu, exception.getMessage(), false);
        }
    }

    private void unsocket(Player player, DungeonRuneSmithMenu menu, int oneBasedSlot) {
        try {
            DungeonRuneDefinition rune = runeService.unsocket(menu.equipment(), oneBasedSlot);
            itemDeliveryService.give(player, List.of(runeService.create(rune.id())));
            menu.refresh(runeService);
            feedback(player, menu, rune.displayName() + " was removed and preserved.", true);
            player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 0.8F, 1.2F);
        } catch (IllegalArgumentException | IllegalStateException exception) {
            feedback(player, menu, exception.getMessage(), false);
        }
    }

    private void repair(Player player, DungeonRuneSmithMenu menu) {
        try {
            runeService.repair(menu.equipment(), menu.rune());
            menu.consumeRune();
            menu.refresh(runeService);
            feedback(player, menu, "The equipment was fully restored.", true);
            player.playSound(player.getLocation(), Sound.BLOCK_GRINDSTONE_USE, 0.8F, 1.15F);
        } catch (IllegalArgumentException | IllegalStateException exception) {
            feedback(player, menu, exception.getMessage(), false);
        }
    }

    private void feedback(Player player, DungeonRuneSmithMenu menu, String message, boolean success) {
        NamedTextColor color = success ? NamedTextColor.GREEN : NamedTextColor.RED;
        menu.setStatus(message, color);
        player.sendActionBar(Component.text(message, color));
        if (!success) {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.65F, 1.0F);
        }
    }

    private void refreshLater(Player player, DungeonRuneSmithMenu menu) {
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (activeMenus.get(player.getUniqueId()) == menu) {
                menu.refresh(runeService);
            }
        });
    }

    private void returnInputs(Player player, DungeonRuneSmithMenu menu) {
        List<ItemStack> returned = menu.drainInputs();
        if (!returned.isEmpty()) {
            itemDeliveryService.give(player, returned);
        }
    }
}
