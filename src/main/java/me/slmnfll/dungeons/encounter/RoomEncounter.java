package me.slmnfll.dungeons.encounter;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class RoomEncounter {
    private final EncounterDefinition definition;
    private final Set<UUID> livingMobIds = new HashSet<>();
    private EncounterRoomState state = EncounterRoomState.PENDING;
    private int currentWave;
    private boolean waveScheduled;

    public RoomEncounter(EncounterDefinition definition) {
        this.definition = definition;
    }

    public EncounterDefinition definition() {
        return definition;
    }

    public EncounterRoomState state() {
        return state;
    }

    public int currentWave() {
        return currentWave;
    }

    public int livingMobCount() {
        return livingMobIds.size();
    }

    public boolean waveScheduled() {
        return waveScheduled;
    }

    public void activate() {
        if (state != EncounterRoomState.PENDING) {
            throw new IllegalStateException("Only a pending encounter can activate.");
        }
        state = EncounterRoomState.ACTIVE;
    }

    public void scheduleWave() {
        if (state != EncounterRoomState.ACTIVE || waveScheduled || !livingMobIds.isEmpty()) {
            throw new IllegalStateException("The next wave cannot be scheduled yet.");
        }
        waveScheduled = true;
    }

    public void beginScheduledWave() {
        if (!waveScheduled || currentWave >= definition.waveCount()) {
            throw new IllegalStateException("There is no scheduled wave to begin.");
        }
        waveScheduled = false;
        currentWave++;
    }

    public void addMob(UUID mobId) {
        livingMobIds.add(mobId);
    }

    public boolean removeMob(UUID mobId) {
        return livingMobIds.remove(mobId);
    }

    public boolean hasAnotherWave() {
        return currentWave < definition.waveCount();
    }

    public void clear() {
        if (state != EncounterRoomState.ACTIVE
                || !livingMobIds.isEmpty()
                || waveScheduled
                || currentWave != definition.waveCount()) {
            throw new IllegalStateException("An encounter cannot clear while its wave is active.");
        }
        state = EncounterRoomState.CLEARED;
    }
}
