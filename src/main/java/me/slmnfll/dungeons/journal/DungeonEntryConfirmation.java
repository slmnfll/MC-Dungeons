package me.slmnfll.dungeons.journal;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

final class DungeonEntryConfirmation {
    private final long confirmationWindowMillis;
    private final Map<UUID, PendingEntry> pendingEntries = new HashMap<>();

    DungeonEntryConfirmation(long confirmationWindowMillis) {
        if (confirmationWindowMillis < 1L) {
            throw new IllegalArgumentException("The confirmation window must be positive.");
        }
        this.confirmationWindowMillis = confirmationWindowMillis;
    }

    boolean confirm(UUID playerId, String dungeonId, long nowMillis) {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(dungeonId, "dungeonId");
        PendingEntry pending = pendingEntries.get(playerId);
        if (pending != null
                && pending.dungeonId().equals(dungeonId)
                && nowMillis <= pending.expiresAtMillis()) {
            pendingEntries.remove(playerId);
            return true;
        }
        pendingEntries.put(
                playerId,
                new PendingEntry(dungeonId, saturatedAdd(nowMillis, confirmationWindowMillis))
        );
        return false;
    }

    void clear(UUID playerId) {
        pendingEntries.remove(playerId);
    }

    private long saturatedAdd(long left, long right) {
        if (left > Long.MAX_VALUE - right) {
            return Long.MAX_VALUE;
        }
        return left + right;
    }

    private record PendingEntry(String dungeonId, long expiresAtMillis) {
    }
}
