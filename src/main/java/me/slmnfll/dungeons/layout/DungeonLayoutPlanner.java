package me.slmnfll.dungeons.layout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public final class DungeonLayoutPlanner {
    private static final int MIN_MAIN_ROOMS = 10;
    private static final int MAIN_ROOM_VARIATION = 3;
    private static final int MIN_BRANCH_ROOMS = 5;
    private static final int BRANCH_ROOM_VARIATION = 4;

    private static final List<RoomType> MAIN_ROOM_TYPES = List.of(
            RoomType.COMBAT,
            RoomType.COMBAT,
            RoomType.PUZZLE,
            RoomType.NPC
    );

    private static final List<RoomType> BRANCH_ROOM_TYPES = List.of(
            RoomType.COMBAT,
            RoomType.NPC,
            RoomType.TREASURE,
            RoomType.SECRET
    );

    public DungeonLayout plan(long seed) {
        Random random = new Random(seed);
        int mainRoomCount = MIN_MAIN_ROOMS + random.nextInt(MAIN_ROOM_VARIATION);
        int branchRoomCount = MIN_BRANCH_ROOMS + random.nextInt(BRANCH_ROOM_VARIATION);

        Map<Integer, MutableRoom> rooms = new HashMap<>();
        boolean mainCombatPlaced = false;
        for (int index = 0; index < mainRoomCount; index++) {
            RoomType type = switch (index) {
                case 0 -> RoomType.START;
                default -> {
                    if (index == mainRoomCount - 1) {
                        yield RoomType.BOSS;
                    }
                    if (index == mainRoomCount - 2 && !mainCombatPlaced) {
                        yield RoomType.COMBAT;
                    }
                    yield MAIN_ROOM_TYPES.get(random.nextInt(MAIN_ROOM_TYPES.size()));
                }
            };
            if (type == RoomType.COMBAT) {
                mainCombatPlaced = true;
            }
            rooms.put(index, new MutableRoom(index, type, new GridPosition(index, 0)));
            if (index > 0) {
                connect(rooms.get(index - 1), rooms.get(index));
            }
        }

        List<Integer> availableAnchors = new ArrayList<>();
        for (int index = 1; index < mainRoomCount - 1; index++) {
            availableAnchors.add(index);
        }
        Collections.shuffle(availableAnchors, random);

        Set<GridPosition> occupied = new HashSet<>();
        rooms.values().forEach(room -> occupied.add(room.position));

        int nextId = mainRoomCount;
        for (int branchIndex = 0; branchIndex < branchRoomCount; branchIndex++) {
            int anchorId = availableAnchors.get(branchIndex % availableAnchors.size());
            MutableRoom anchor = rooms.get(anchorId);
            int direction = branchIndex < availableAnchors.size()
                    ? (random.nextBoolean() ? 1 : -1)
                    : (branchIndex % 2 == 0 ? 1 : -1);
            GridPosition candidate = new GridPosition(anchor.position.x(), direction);

            if (occupied.contains(candidate)) {
                direction *= -1;
                candidate = new GridPosition(anchor.position.x(), direction);
            }
            if (occupied.contains(candidate)) {
                continue;
            }

            MutableRoom branch = new MutableRoom(
                    nextId++,
                    BRANCH_ROOM_TYPES.get(random.nextInt(BRANCH_ROOM_TYPES.size())),
                    candidate
            );
            rooms.put(branch.id, branch);
            occupied.add(candidate);
            connect(anchor, branch);
        }

        List<RoomNode> finalizedRooms = rooms.values().stream()
                .sorted((left, right) -> Integer.compare(left.id, right.id))
                .map(MutableRoom::toRoomNode)
                .toList();

        return new DungeonLayout(seed, finalizedRooms, 0, mainRoomCount - 1);
    }

    private static void connect(MutableRoom left, MutableRoom right) {
        left.connections.add(right.id);
        right.connections.add(left.id);
    }

    private static final class MutableRoom {
        private final int id;
        private final RoomType type;
        private final GridPosition position;
        private final Set<Integer> connections = new HashSet<>();

        private MutableRoom(int id, RoomType type, GridPosition position) {
            this.id = id;
            this.type = type;
            this.position = position;
        }

        private RoomNode toRoomNode() {
            return new RoomNode(id, type, position, connections);
        }
    }
}
