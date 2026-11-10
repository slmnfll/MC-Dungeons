package me.slmnfll.dungeons.instance;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public final class DungeonRunStartedEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final DungeonSession session;

    public DungeonRunStartedEvent(Player player, DungeonSession session) {
        this.player = Objects.requireNonNull(player, "player");
        this.session = Objects.requireNonNull(session, "session");
    }

    public Player player() {
        return player;
    }

    public DungeonSession session() {
        return session;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static @NotNull HandlerList getHandlerList() {
        return HANDLERS;
    }
}
