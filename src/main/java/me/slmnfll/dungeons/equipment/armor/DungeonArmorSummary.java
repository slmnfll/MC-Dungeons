package me.slmnfll.dungeons.equipment.armor;

import java.util.Map;
import java.util.Objects;

public record DungeonArmorSummary(
        double powerBonus,
        Map<DungeonArmorSet, Integer> equippedPieces
) {
    private static final double TWO_PIECE_DAMAGE_BONUS = 0.08D;
    private static final double FOUR_PIECE_DAMAGE_BONUS = 0.20D;

    public DungeonArmorSummary {
        if (powerBonus < 0.0D) {
            throw new IllegalArgumentException("Armor power cannot be negative.");
        }
        equippedPieces = Map.copyOf(Objects.requireNonNull(equippedPieces, "equippedPieces"));
    }

    public static DungeonArmorSummary empty() {
        return new DungeonArmorSummary(0.0D, Map.of());
    }

    public int pieces(DungeonArmorSet set) {
        return equippedPieces.getOrDefault(set, 0);
    }

    public double outgoingDamageMultiplier(boolean projectile) {
        double setBonus = 0.0D;
        if (projectile) {
            setBonus = damageBonus(DungeonArmorSet.TRAILSEEKER);
        } else {
            setBonus = damageBonus(DungeonArmorSet.WARBORN);
        }
        return 1.0D + powerBonus + setBonus;
    }

    public double incomingDamageMultiplier() {
        int bulwarkPieces = pieces(DungeonArmorSet.BULWARK);
        if (bulwarkPieces >= 4) {
            return 0.85D;
        }
        if (bulwarkPieces >= 2) {
            return 0.95D;
        }
        return 1.0D;
    }

    private double damageBonus(DungeonArmorSet set) {
        int pieces = pieces(set);
        if (pieces >= 4) {
            return FOUR_PIECE_DAMAGE_BONUS;
        }
        if (pieces >= 2) {
            return TWO_PIECE_DAMAGE_BONUS;
        }
        return 0.0D;
    }
}
