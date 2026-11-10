package me.slmnfll.dungeons.content;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public final class DungeonRiskService {
    private final NamespacedKey runIdKey;
    private final DungeonLootLossPolicy lootLossPolicy = new DungeonLootLossPolicy();
    private final DungeonRiskLore riskLore = new DungeonRiskLore();

    public DungeonRiskService(JavaPlugin plugin) {
        this.runIdKey = new NamespacedKey(plugin, "at_risk_run");
    }

    public List<ItemStack> markAtRisk(List<ItemStack> items, UUID runId) {
        for (ItemStack item : items) {
            ItemMeta meta = item.getItemMeta();
            meta.getPersistentDataContainer().set(runIdKey, PersistentDataType.STRING, runId.toString());
            meta.lore(riskLore.mark(meta.lore()));
            item.setItemMeta(meta);
        }
        return items;
    }

    public void secure(Player player, UUID runId) {
        updateInventory(player, runId, false);
    }

    public void discard(Player player, UUID runId) {
        updateInventory(player, runId, true);
    }

    public DungeonDeathLootResult resolveDeath(Player player, UUID runId) {
        PlayerInventory inventory = player.getInventory();
        String expectedRunId = runId.toString();
        List<ItemStack> droppedStacks = new ArrayList<>();
        int keptAmount = 0;
        int lostAmount = 0;

        for (int slot = 0; slot < inventory.getSize(); slot++) {
            ItemStack item = inventory.getItem(slot);
            if (!belongsToRun(item, expectedRunId)) {
                continue;
            }

            int lostFromStack = lootLossPolicy.lostAmount(item.getAmount(), ThreadLocalRandom.current());
            int keptFromStack = item.getAmount() - lostFromStack;
            if (lostFromStack > 0) {
                ItemStack dropped = item.clone();
                dropped.setAmount(lostFromStack);
                droppedStacks.add(dropped);
                lostAmount += lostFromStack;
            }

            if (keptFromStack == 0) {
                inventory.setItem(slot, null);
                continue;
            }
            item.setAmount(keptFromStack);
            clearRiskTag(item);
            inventory.setItem(slot, item);
            keptAmount += keptFromStack;
        }
        return new DungeonDeathLootResult(droppedStacks, keptAmount, lostAmount);
    }

    public DungeonDeathLootResult resolveTotalLoss(Player player, UUID runId) {
        PlayerInventory inventory = player.getInventory();
        String expectedRunId = runId.toString();
        int lostAmount = 0;
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            ItemStack item = inventory.getItem(slot);
            if (!belongsToRun(item, expectedRunId)) {
                continue;
            }
            lostAmount += item.getAmount();
            inventory.setItem(slot, null);
        }
        return new DungeonDeathLootResult(List.of(), 0, lostAmount);
    }

    public int discardAllAtRisk(Player player) {
        PlayerInventory inventory = player.getInventory();
        int discarded = 0;
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            ItemStack item = inventory.getItem(slot);
            if (item == null || item.getType().isAir() || !item.hasItemMeta()) {
                continue;
            }
            if (item.getItemMeta().getPersistentDataContainer().has(runIdKey, PersistentDataType.STRING)) {
                inventory.setItem(slot, null);
                discarded++;
            }
        }
        return discarded;
    }

    private void updateInventory(Player player, UUID runId, boolean discard) {
        PlayerInventory inventory = player.getInventory();
        String expectedRunId = runId.toString();
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            ItemStack item = inventory.getItem(slot);
            if (!belongsToRun(item, expectedRunId)) {
                continue;
            }
            if (discard) {
                inventory.setItem(slot, null);
            } else {
                clearRiskTag(item);
            }
        }
    }

    private void clearRiskTag(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().remove(runIdKey);
        meta.lore(riskLore.clear(meta.lore()));
        item.setItemMeta(meta);
    }

    private boolean belongsToRun(ItemStack item, String expectedRunId) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) {
            return false;
        }
        String runId = item.getItemMeta().getPersistentDataContainer().get(runIdKey, PersistentDataType.STRING);
        return expectedRunId.equals(runId);
    }
}
