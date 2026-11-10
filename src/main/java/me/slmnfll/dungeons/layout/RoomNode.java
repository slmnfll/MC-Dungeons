package me.slmnfll.dungeons.layout;

import java.util.Set;

public record RoomNode(int id, RoomType type, GridPosition position, Set<Integer> connections) {
    public RoomNode {
        connections = Set.copyOf(connections);
    }
}
