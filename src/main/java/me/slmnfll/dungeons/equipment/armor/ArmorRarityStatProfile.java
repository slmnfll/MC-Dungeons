package me.slmnfll.dungeons.equipment.armor;

import me.slmnfll.dungeons.content.ItemRarity;

import java.util.Random;

public record ArmorRarityStatProfile(
        double minimumPrimaryMultiplier,
        double maximumPrimaryMultiplier,
        double minimumDurabilityMultiplier,
        double maximumDurabilityMultiplier
) {
    public static ArmorRarityStatProfile forRarity(ItemRarity rarity) {
        return switch (rarity) {
            case COMMON -> new ArmorRarityStatProfile(0.92D, 1.00D, 0.90D, 1.00D);
            case UNCOMMON -> new ArmorRarityStatProfile(1.00D, 1.13D, 1.00D, 1.14D);
            case RARE -> new ArmorRarityStatProfile(1.14D, 1.32D, 1.12D, 1.32D);
            case EPIC -> new ArmorRarityStatProfile(1.32D, 1.57D, 1.30D, 1.58D);
            case LEGENDARY -> new ArmorRarityStatProfile(1.58D, 1.88D, 1.55D, 1.90D);
            case MYTHICAL -> new ArmorRarityStatProfile(2.05D, 2.05D, 2.20D, 2.20D);
            case EXTINCT -> new ArmorRarityStatProfile(2.55D, 2.55D, 1.00D, 1.00D);
        };
    }

    public DungeonArmorStats roll(DungeonArmorDefinition definition, long seed) {
        if (definition.fixedStats()) {
            return new DungeonArmorStats(
                    roundOneDecimal(definition.baseDefense()),
                    roundOneDecimal(definition.baseVitality()),
                    roundThreeDecimals(definition.basePower()),
                    definition.rarity() == ItemRarity.EXTINCT ? 0 : definition.baseDurability()
            );
        }

        Random random = new Random(seed);
        double defense = definition.baseDefense() * between(random, minimumPrimaryMultiplier, maximumPrimaryMultiplier);
        double vitality = definition.baseVitality() * between(random, minimumPrimaryMultiplier, maximumPrimaryMultiplier);
        double power = definition.basePower() * between(random, minimumPrimaryMultiplier, maximumPrimaryMultiplier);
        int durability = definition.rarity() == ItemRarity.EXTINCT
                ? 0
                : Math.max(1, (int) Math.round(definition.baseDurability()
                        * between(random, minimumDurabilityMultiplier, maximumDurabilityMultiplier)));
        return new DungeonArmorStats(
                roundOneDecimal(defense),
                roundOneDecimal(vitality),
                roundThreeDecimals(power),
                durability
        );
    }

    private double between(Random random, double minimum, double maximum) {
        return minimum == maximum ? minimum : minimum + random.nextDouble() * (maximum - minimum);
    }

    private double roundOneDecimal(double value) {
        return Math.round(value * 10.0D) / 10.0D;
    }

    private double roundThreeDecimals(double value) {
        return Math.round(value * 1_000.0D) / 1_000.0D;
    }
}
