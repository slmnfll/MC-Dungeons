package me.slmnfll.dungeons.profile;

import me.slmnfll.dungeons.affinity.Affinity;

import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class DungeonProfile {
    private final UUID playerId;
    private int level;
    private long experienceIntoLevel;
    private final EnumSet<Affinity> claimedStarters;
    private Affinity selectedAffinity;
    private int completedExpeditions;

    public DungeonProfile(UUID playerId, int level, long experienceIntoLevel) {
        this(playerId, level, experienceIntoLevel, Set.of(), null);
    }

    public DungeonProfile(
            UUID playerId,
            int level,
            long experienceIntoLevel,
            Set<Affinity> claimedStarters
    ) {
        this(playerId, level, experienceIntoLevel, claimedStarters, null);
    }

    public DungeonProfile(
            UUID playerId,
            int level,
            long experienceIntoLevel,
            Set<Affinity> claimedStarters,
            Affinity selectedAffinity
    ) {
        this(playerId, level, experienceIntoLevel, claimedStarters, selectedAffinity, 0);
    }

    public DungeonProfile(
            UUID playerId,
            int level,
            long experienceIntoLevel,
            Set<Affinity> claimedStarters,
            Affinity selectedAffinity,
            int completedExpeditions
    ) {
        if (level < 1 || experienceIntoLevel < 0L || completedExpeditions < 0) {
            throw new IllegalArgumentException("Invalid Dungeon profile values.");
        }
        this.playerId = playerId;
        this.level = level;
        this.experienceIntoLevel = experienceIntoLevel;
        this.claimedStarters = claimedStarters.isEmpty()
                ? EnumSet.noneOf(Affinity.class)
                : EnumSet.copyOf(claimedStarters);
        this.selectedAffinity = selectedAffinity;
        this.completedExpeditions = completedExpeditions;
    }

    public static DungeonProfile newProfile(UUID playerId) {
        return new DungeonProfile(playerId, 1, 0L);
    }

    public UUID playerId() {
        return playerId;
    }

    public int level() {
        return level;
    }

    public long experienceIntoLevel() {
        return experienceIntoLevel;
    }

    public Set<Affinity> claimedStarters() {
        return Set.copyOf(claimedStarters);
    }

    public boolean hasClaimedStarter(Affinity affinity) {
        return claimedStarters.contains(affinity);
    }

    public boolean claimStarter(Affinity affinity) {
        return claimedStarters.add(affinity);
    }

    public Optional<Affinity> selectedAffinity() {
        return Optional.ofNullable(selectedAffinity);
    }

    public boolean selectAffinity(Affinity affinity) {
        if (selectedAffinity == affinity) {
            return false;
        }
        selectedAffinity = affinity;
        return true;
    }

    public int completedExpeditions() {
        return completedExpeditions;
    }

    public int recordCompletedExpedition() {
        if (completedExpeditions < Integer.MAX_VALUE) {
            completedExpeditions++;
        }
        return completedExpeditions;
    }

    public void resetForNewAdventure() {
        level = 1;
        experienceIntoLevel = 0L;
        completedExpeditions = 0;
        claimedStarters.clear();
    }

    public ExperienceAward addExperience(long amount, DungeonLevelCurve levelCurve) {
        if (amount < 0L) {
            throw new IllegalArgumentException("Dungeon XP awards cannot be negative.");
        }

        experienceIntoLevel += amount;
        int levelsGained = 0;
        while (level < Integer.MAX_VALUE) {
            long required = levelCurve.experienceRequiredForNextLevel(level);
            if (experienceIntoLevel < required) {
                break;
            }
            experienceIntoLevel -= required;
            level++;
            levelsGained++;
        }
        return new ExperienceAward(amount, levelsGained, level, experienceIntoLevel);
    }

    public record ExperienceAward(long experience, int levelsGained, int newLevel, long experienceIntoLevel) {
    }
}
