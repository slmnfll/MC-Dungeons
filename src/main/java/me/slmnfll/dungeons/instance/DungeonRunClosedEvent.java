package me.slmnfll.dungeons.instance;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.UUID;

public final class DungeonRunClosedEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final DungeonInstanceState finalState;
    private final UUID instanceId;
    private final DungeonRunType runType;
    private final DungeonRunEndReason endReason;

    public DungeonRunClosedEvent(
            Player player,
            DungeonInstanceState finalState,
            UUID instanceId,
            DungeonRunType runType,
            DungeonRunEndReason endReason
    ) {
        this.player = Objects.requireNonNull(player, "player");
        this.finalState = Objects.requireNonNull(finalState, "finalState");
        this.instanceId = Objects.requireNonNull(instanceId, "instanceId");
        this.runType = Objects.requireNonNull(runType, "runType");
        this.endReason = Objects.requireNonNull(endReason, "endReason");
    }

    public Player player() {
        return player;
    }

    public DungeonInstanceState finalState() {
        return finalState;
    }

    public UUID instanceId() {
        return instanceId;
    }

    public DungeonRunType runType() {
        return runType;
    }

    public DungeonRunEndReason endReason() {
        return endReason;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static @NotNull HandlerList getHandlerList() {
        return HANDLERS;
    }
}
