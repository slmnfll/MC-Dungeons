package me.slmnfll.dungeons.content;

import org.bukkit.Material;

import java.util.Random;

public record LootEntry(
        String id,
        String displayName,
        Material material,
        ItemRarity rarity,
        int minimumAmount,
        int maximumAmount,
        int weight
) {
    public LootEntry {
        if (minimumAmount < 1 || maximumAmount < minimumAmount || weight < 1) {
            throw new IllegalArgumentException("Invalid loot entry amounts or weight.");
        }
    }

    public int rollAmount(Random random) {
        return minimumAmount + random.nextInt(maximumAmount - minimumAmount + 1);
    }
}
