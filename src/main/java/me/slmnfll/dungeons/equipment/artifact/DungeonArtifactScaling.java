package me.slmnfll.dungeons.equipment.artifact;

import me.slmnfll.dungeons.affinity.Affinity;
import me.slmnfll.dungeons.affinity.DungeonPowerScaling;

public final class DungeonArtifactScaling {
    private final DungeonPowerScaling powerScaling = new DungeonPowerScaling();

    public double potency(double basePower, Affinity selectedAffinity, Affinity artifactAffinity) {
        if (basePower <= 0.0D) {
            throw new IllegalArgumentException("Artifact power must be positive.");
        }
        return basePower * affinityMultiplier(selectedAffinity, artifactAffinity);
    }

    public int durationTicks(int baseDurationTicks, Affinity selectedAffinity, Affinity artifactAffinity) {
        if (baseDurationTicks < 0) {
            throw new IllegalArgumentException("Artifact duration cannot be negative.");
        }
        return (int) Math.round(baseDurationTicks * affinityMultiplier(selectedAffinity, artifactAffinity));
    }

    public double damage(
            double baseDamage,
            int dungeonLevel,
            Affinity selectedAffinity,
            Affinity artifactAffinity
    ) {
        return potency(baseDamage, selectedAffinity, artifactAffinity)
                * powerScaling.levelDamageMultiplier(dungeonLevel);
    }

    private double affinityMultiplier(Affinity selectedAffinity, Affinity artifactAffinity) {
        return selectedAffinity == artifactAffinity ? 1.0D + Affinity.EFFECTIVENESS_BONUS : 1.0D;
    }
}
