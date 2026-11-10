package me.slmnfll.dungeons.profile;

import java.util.UUID;

public interface DungeonProfileRepository {
    DungeonProfile load(UUID playerId);

    void save(DungeonProfile profile);

    void flush();
}
