package me.slmnfll.dungeons.layout;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

public final class DungeonLayoutValidator {
    public void validate(DungeonLayout layout) {
        if (layout.rooms().isEmpty()) {
            throw new IllegalArgumentException("A dungeon layout cannot be empty.");
        }

        Set<Integer> ids = new HashSet<>();
        Set<GridPosition> positions = new HashSet<>();
        for (RoomNode room : layout.rooms()) {
            if (!ids.add(room.id())) {
                throw new IllegalArgumentException("Duplicate room id: " + room.id());
            }
            if (!positions.add(room.position())) {
                throw new IllegalArgumentException("Two rooms occupy " + room.position());
            }
        }

        RoomNode start = layout.roomById(layout.startRoomId())
                .orElseThrow(() -> new IllegalArgumentException("The start room does not exist."));
        RoomNode boss = layout.roomById(layout.bossRoomId())
                .orElseThrow(() -> new IllegalArgumentException("The boss room does not exist."));
        if (start.type() != RoomType.START || boss.type() != RoomType.BOSS) {
            throw new IllegalArgumentException("The designated start and boss rooms have invalid types.");
        }

        for (RoomNode room : layout.rooms()) {
            for (int connectedId : room.connections()) {
                RoomNode connected = layout.roomById(connectedId)
                        .orElseThrow(() -> new IllegalArgumentException("Room " + room.id() + " has a missing connection."));
                if (!connected.connections().contains(room.id())) {
                    throw new IllegalArgumentException("Connection " + room.id() + " -> " + connectedId + " is not bidirectional.");
                }
            }
        }

        Set<Integer> visited = new HashSet<>();
        ArrayDeque<Integer> remaining = new ArrayDeque<>();
        remaining.add(layout.startRoomId());
        while (!remaining.isEmpty()) {
            int roomId = remaining.removeFirst();
            if (!visited.add(roomId)) {
                continue;
            }
            layout.roomById(roomId).orElseThrow().connections().stream()
                    .filter(id -> !visited.contains(id))
                    .forEach(remaining::addLast);
        }

        if (visited.size() != layout.rooms().size()) {
            throw new IllegalArgumentException("The dungeon layout contains unreachable rooms.");
        }
    }
}
