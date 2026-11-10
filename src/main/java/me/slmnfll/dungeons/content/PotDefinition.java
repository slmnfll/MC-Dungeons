package me.slmnfll.dungeons.content;

import java.util.Optional;

public record PotDefinition(long lootSeed, boolean containsKey, boolean trapped, ItemRarity boostedRarity) {
    public Optional<ItemRarity> rarityHint() {
        return Optional.ofNullable(boostedRarity);
    }
}
