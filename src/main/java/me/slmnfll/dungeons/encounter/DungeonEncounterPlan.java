package me.slmnfll.dungeons.encounter;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

public record DungeonEncounterPlan(
        Map<Integer, EncounterDefinition> encounters,
        Set<Integer> requiredCombatRoomIds,
        int bossRoomId
) {
    public DungeonEncounterPlan {
        encounters = Map.copyOf(encounters);
        requiredCombatRoomIds = Set.copyOf(requiredCombatRoomIds);
    }

    public Optional<EncounterDefinition> encounter(int roomId) {
        return Optional.ofNullable(encounters.get(roomId));
    }
}
