package me.slmnfll.dungeons.encounter;

import me.slmnfll.dungeons.layout.DungeonLayout;
import me.slmnfll.dungeons.layout.RoomNode;
import me.slmnfll.dungeons.layout.RoomType;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public final class DungeonEncounterPlanner {
    private static final long ENCOUNTER_SEED_SALT = 0x0D06E0A71L;

    public DungeonEncounterPlan plan(DungeonLayout layout) {
        Traversal traversal = traverse(layout);
        Set<Integer> mainRoute = mainRoute(layout, traversal.predecessors());
        Map<Integer, EncounterDefinition> encounters = new LinkedHashMap<>();
        Set<Integer> requiredCombatRooms = new HashSet<>();

        for (RoomNode room : layout.rooms()) {
            if (room.type() != RoomType.COMBAT && room.type() != RoomType.BOSS) {
                continue;
            }

            boolean boss = room.type() == RoomType.BOSS;
            boolean required = !boss && mainRoute.contains(room.id());
            int waves = boss ? 1 : 1 + new Random(
                    layout.seed() ^ ENCOUNTER_SEED_SALT ^ ((long) room.id() << 32)
            ).nextInt(3);
            encounters.put(room.id(), new EncounterDefinition(
                    room.id(),
                    required,
                    boss,
                    waves,
                    traversal.distances().getOrDefault(room.id(), 0)
            ));
            if (required) {
                requiredCombatRooms.add(room.id());
            }
        }

        return new DungeonEncounterPlan(encounters, requiredCombatRooms, layout.bossRoomId());
    }

    private Traversal traverse(DungeonLayout layout) {
        Map<Integer, Integer> distances = new HashMap<>();
        Map<Integer, Integer> predecessors = new HashMap<>();
        ArrayDeque<Integer> queue = new ArrayDeque<>();
        distances.put(layout.startRoomId(), 0);
        queue.add(layout.startRoomId());

        while (!queue.isEmpty()) {
            int roomId = queue.removeFirst();
            RoomNode room = layout.roomById(roomId).orElseThrow();
            for (int connectedId : room.connections()) {
                if (distances.containsKey(connectedId)) {
                    continue;
                }
                distances.put(connectedId, distances.get(roomId) + 1);
                predecessors.put(connectedId, roomId);
                queue.addLast(connectedId);
            }
        }
        return new Traversal(distances, predecessors);
    }

    private Set<Integer> mainRoute(DungeonLayout layout, Map<Integer, Integer> predecessors) {
        Set<Integer> route = new HashSet<>();
        int current = layout.bossRoomId();
        route.add(current);
        while (current != layout.startRoomId()) {
            Integer predecessor = predecessors.get(current);
            if (predecessor == null) {
                throw new IllegalArgumentException("The boss room is unreachable from the start.");
            }
            current = predecessor;
            route.add(current);
        }
        return route;
    }

    private record Traversal(Map<Integer, Integer> distances, Map<Integer, Integer> predecessors) {
    }
}
