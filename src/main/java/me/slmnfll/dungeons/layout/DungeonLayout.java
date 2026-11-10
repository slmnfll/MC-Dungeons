package me.slmnfll.dungeons.layout;

import java.util.List;
import java.util.Optional;

public record DungeonLayout(long seed, List<RoomNode> rooms, int startRoomId, int bossRoomId) {
    public DungeonLayout {
        rooms = List.copyOf(rooms);
    }

    public Optional<RoomNode> roomById(int roomId) {
        return rooms.stream().filter(room -> room.id() == roomId).findFirst();
    }
}
