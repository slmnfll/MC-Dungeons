package me.slmnfll.dungeons.equipment.weapon;

import me.slmnfll.dungeons.content.ItemRarity;

import java.util.Random;

public record RarityStatProfile(
        double minimumDamageMultiplier,
        double maximumDamageMultiplier,
        double minimumSpeedMultiplier,
        double maximumSpeedMultiplier,
        double minimumDurabilityMultiplier,
        double maximumDurabilityMultiplier
) {
    public static RarityStatProfile forRarity(ItemRarity rarity) {
        return switch (rarity) {
            case COMMON -> new RarityStatProfile(0.92D, 1.00D, 0.96D, 1.02D, 0.90D, 1.00D);
            case UNCOMMON -> new RarityStatProfile(1.00D, 1.13D, 0.98D, 1.05D, 1.00D, 1.14D);
            case RARE -> new RarityStatProfile(1.14D, 1.32D, 1.00D, 1.10D, 1.12D, 1.32D);
            case EPIC -> new RarityStatProfile(1.32D, 1.57D, 1.04D, 1.15D, 1.30D, 1.58D);
            case LEGENDARY -> new RarityStatProfile(1.58D, 1.88D, 1.08D, 1.20D, 1.55D, 1.90D);
            case MYTHICAL -> new RarityStatProfile(2.05D, 2.05D, 1.20D, 1.20D, 2.20D, 2.20D);
            case EXTINCT -> new RarityStatProfile(2.55D, 2.55D, 1.28D, 1.28D, 1.00D, 1.00D);
        };
    }

    public DungeonWeaponStats roll(DungeonWeaponDefinition definition, long seed) {
        if (definition.fixedStats()) {
            return new DungeonWeaponStats(
                    roundOneDecimal(definition.baseDamage()),
                    roundTwoDecimals(definition.baseSpeed()),
                    definition.rarity() == ItemRarity.EXTINCT ? 0 : definition.baseDurability()
            );
        }

        Random random = new Random(seed);
        double damage = definition.baseDamage() * between(
                random,
                minimumDamageMultiplier,
                maximumDamageMultiplier
        );
        double speed = definition.baseSpeed() * between(
                random,
                minimumSpeedMultiplier,
                maximumSpeedMultiplier
        );
        int durability = definition.rarity() == ItemRarity.EXTINCT
                ? 0
                : Math.max(1, (int) Math.round(definition.baseDurability() * between(
                        random,
                        minimumDurabilityMultiplier,
                        maximumDurabilityMultiplier
                )));
        return new DungeonWeaponStats(roundOneDecimal(damage), roundTwoDecimals(speed), durability);
    }

    private double between(Random random, double minimum, double maximum) {
        if (minimum == maximum) {
            return minimum;
        }
        return minimum + random.nextDouble() * (maximum - minimum);
    }

    private double roundOneDecimal(double value) {
        return Math.round(value * 10.0D) / 10.0D;
    }

    private double roundTwoDecimals(double value) {
        return Math.round(value * 100.0D) / 100.0D;
    }
}
