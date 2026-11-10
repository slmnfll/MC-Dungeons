package me.slmnfll.dungeons.instance;

import java.util.EnumSet;
import java.util.Set;

public enum DungeonInstanceState {
    PREPARING,
    GENERATING,
    ACTIVE,
    BOSS_DEFEATED,
    COMPLETED,
    FAILED,
    CLOSING,
    CLOSED;

    private Set<DungeonInstanceState> allowedNextStates() {
        return switch (this) {
            case PREPARING -> EnumSet.of(GENERATING, FAILED);
            case GENERATING -> EnumSet.of(ACTIVE, FAILED);
            case ACTIVE -> EnumSet.of(BOSS_DEFEATED, FAILED, CLOSING);
            case BOSS_DEFEATED -> EnumSet.of(COMPLETED, FAILED, CLOSING);
            case COMPLETED, FAILED -> EnumSet.of(CLOSING);
            case CLOSING -> EnumSet.of(CLOSED);
            case CLOSED -> EnumSet.noneOf(DungeonInstanceState.class);
        };
    }

    public boolean canTransitionTo(DungeonInstanceState nextState) {
        return allowedNextStates().contains(nextState);
    }
}
