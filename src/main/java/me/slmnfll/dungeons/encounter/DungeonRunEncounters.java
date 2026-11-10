package me.slmnfll.dungeons.encounter;

import me.slmnfll.dungeons.content.BlockPosition;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class DungeonRunEncounters {
    private final DungeonEncounterPlan plan;
    private final Map<Integer, RoomEncounter> rooms = new LinkedHashMap<>();
    private boolean bossUnlocked;
    private long exitDeadlineMillis;
    private Set<BlockPosition> exitPortalBlocks = Set.of();

    public DungeonRunEncounters(DungeonEncounterPlan plan) {
        this.plan = plan;
        plan.encounters().values().forEach(definition -> rooms.put(
                definition.roomId(),
                new RoomEncounter(definition)
        ));
        bossUnlocked = plan.requiredCombatRoomIds().isEmpty();
    }

    public DungeonEncounterPlan plan() {
        return plan;
    }

    public Optional<RoomEncounter> room(int roomId) {
        return Optional.ofNullable(rooms.get(roomId));
    }

    public boolean bossUnlocked() {
        return bossUnlocked;
    }

    public boolean refreshBossUnlock() {
        if (bossUnlocked) {
            return false;
        }
        boolean allCleared = plan.requiredCombatRoomIds().stream()
                .map(rooms::get)
                .allMatch(room -> room != null && room.state() == EncounterRoomState.CLEARED);
        if (allCleared) {
            bossUnlocked = true;
            return true;
        }
        return false;
    }

    public Optional<RoomEncounter> activeRoom() {
        return rooms.values().stream().filter(room -> room.state() == EncounterRoomState.ACTIVE).findFirst();
    }

    public void beginExitCountdown(long deadlineMillis, Set<BlockPosition> portalBlocks) {
        this.exitDeadlineMillis = deadlineMillis;
        this.exitPortalBlocks = Set.copyOf(portalBlocks);
    }

    public long exitDeadlineMillis() {
        return exitDeadlineMillis;
    }

    public Set<BlockPosition> exitPortalBlocks() {
        return exitPortalBlocks;
    }
}
