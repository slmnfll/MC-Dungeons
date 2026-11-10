package me.slmnfll.dungeons.profile;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class DungeonProgressionOverrideRegistry {
    private final Map<UUID, Integer> effectiveLevels = new HashMap<>();

    public void beginTutorial(UUID playerId) {
        effectiveLevels.put(playerId, 1);
    }

    public void clear(UUID playerId) {
        effectiveLevels.remove(playerId);
    }

    public int effectiveLevel(UUID playerId, int persistentLevel) {
        return effectiveLevels.getOrDefault(playerId, persistentLevel);
    }

    public boolean experienceEnabled(UUID playerId) {
        return !effectiveLevels.containsKey(playerId);
    }
}
