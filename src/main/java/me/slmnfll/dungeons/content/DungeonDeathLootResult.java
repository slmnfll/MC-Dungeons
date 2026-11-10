package me.slmnfll.dungeons.content;

import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Objects;

public record DungeonDeathLootResult(
        List<ItemStack> droppedStacks,
        int keptAmount,
        int lostAmount
) {
    public DungeonDeathLootResult {
        droppedStacks = List.copyOf(Objects.requireNonNull(droppedStacks, "droppedStacks"));
        if (keptAmount < 0 || lostAmount < 0) {
            throw new IllegalArgumentException("Resolved loot amounts cannot be negative.");
        }
    }

    public static DungeonDeathLootResult empty() {
        return new DungeonDeathLootResult(List.of(), 0, 0);
    }
}
