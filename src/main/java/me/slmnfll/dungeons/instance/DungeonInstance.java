package me.slmnfll.dungeons.instance;

import me.slmnfll.dungeons.affinity.Affinity;
import me.slmnfll.dungeons.layout.DungeonLayout;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class DungeonInstance {
    private final UUID id;
    private final UUID ownerId;
    private final Affinity affinity;
    private final DungeonRunType runType;
    private final DungeonLayout layout;
    private final Instant createdAt;
    private DungeonInstanceState state;

    public DungeonInstance(UUID id, UUID ownerId, Affinity affinity, DungeonLayout layout) {
        this(id, ownerId, affinity, layout, DungeonRunType.ADVENTURE);
    }

    public DungeonInstance(
            UUID id,
            UUID ownerId,
            Affinity affinity,
            DungeonLayout layout,
            DungeonRunType runType
    ) {
        this.id = Objects.requireNonNull(id, "id");
        this.ownerId = Objects.requireNonNull(ownerId, "ownerId");
        this.affinity = Objects.requireNonNull(affinity, "affinity");
        this.runType = Objects.requireNonNull(runType, "runType");
        this.layout = Objects.requireNonNull(layout, "layout");
        this.createdAt = Instant.now();
        this.state = DungeonInstanceState.PREPARING;
    }

    public UUID id() {
        return id;
    }

    public UUID ownerId() {
        return ownerId;
    }

    public Affinity affinity() {
        return affinity;
    }

    public DungeonRunType runType() {
        return runType;
    }

    public DungeonLayout layout() {
        return layout;
    }

    public long seed() {
        return layout.seed();
    }

    public Instant createdAt() {
        return createdAt;
    }

    public DungeonInstanceState state() {
        return state;
    }

    public String worldName() {
        return "dungeon_instance_" + id.toString().replace("-", "");
    }

    public void transitionTo(DungeonInstanceState nextState) {
        Objects.requireNonNull(nextState, "nextState");
        if (!state.canTransitionTo(nextState)) {
            throw new IllegalStateException("Invalid dungeon state transition: " + state + " -> " + nextState);
        }
        state = nextState;
    }
}
