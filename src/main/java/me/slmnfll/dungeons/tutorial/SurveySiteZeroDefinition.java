package me.slmnfll.dungeons.tutorial;

import me.slmnfll.dungeons.content.BlockPosition;
import me.slmnfll.dungeons.content.DungeonContentPlan;
import me.slmnfll.dungeons.content.ItemRarity;
import me.slmnfll.dungeons.content.LockedChestDefinition;
import me.slmnfll.dungeons.content.PotDefinition;
import me.slmnfll.dungeons.encounter.DungeonEncounterPlan;
import me.slmnfll.dungeons.encounter.EncounterDefinition;
import me.slmnfll.dungeons.layout.DungeonLayout;
import me.slmnfll.dungeons.layout.GridPosition;
import me.slmnfll.dungeons.layout.RoomNode;
import me.slmnfll.dungeons.layout.RoomType;
import me.slmnfll.dungeons.world.DungeonGeometry;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class SurveySiteZeroDefinition {
    public static final long SEED = 0x5A17E_000L;
    public static final int POT_COUNT = 5;
    public static final int KEY_COUNT = 1;
    public static final int CHEST_COUNT = 1;
    public static final int COMBAT_ROOM_ID = 4;
    public static final int BOSS_ROOM_ID = 5;

    public DungeonLayout layout() {
        return new DungeonLayout(
                SEED,
                List.of(
                        room(0, RoomType.START, 0, Set.of(1)),
                        room(1, RoomType.PUZZLE, 1, Set.of(0, 2)),
                        room(2, RoomType.PUZZLE, 2, Set.of(1, 3)),
                        room(3, RoomType.TREASURE, 3, Set.of(2, 4)),
                        room(COMBAT_ROOM_ID, RoomType.COMBAT, 4, Set.of(3, BOSS_ROOM_ID)),
                        room(BOSS_ROOM_ID, RoomType.BOSS, 5, Set.of(COMBAT_ROOM_ID))
                ),
                0,
                BOSS_ROOM_ID
        );
    }

    public DungeonContentPlan contentPlan() {
        Map<BlockPosition, PotDefinition> pots = new LinkedHashMap<>();

        // Room one teaches basic pot interaction without allowing the key lesson to be skipped.
        pots.put(position(1, -3, -4), pot(0x5101L, false, null));
        pots.put(position(1, 3, 4), pot(0x5102L, false, ItemRarity.UNCOMMON));

        // The following room is the deterministic key hunt.
        pots.put(position(2, -5, -4), pot(0x5201L, false, null));
        pots.put(position(2, 0, 4), pot(0x5202L, true, ItemRarity.RARE));
        pots.put(position(2, 5, -4), pot(0x5203L, false, null));

        Map<BlockPosition, LockedChestDefinition> chests = new LinkedHashMap<>();
        chests.put(position(3, 0, 0), new LockedChestDefinition(3, 0x5301L));
        return new DungeonContentPlan(pots, chests);
    }

    public DungeonEncounterPlan encounterPlan() {
        Map<Integer, EncounterDefinition> encounters = new LinkedHashMap<>();
        encounters.put(COMBAT_ROOM_ID, new EncounterDefinition(
                COMBAT_ROOM_ID,
                true,
                false,
                1,
                COMBAT_ROOM_ID
        ));
        encounters.put(BOSS_ROOM_ID, new EncounterDefinition(
                BOSS_ROOM_ID,
                false,
                true,
                1,
                BOSS_ROOM_ID
        ));
        return new DungeonEncounterPlan(encounters, Set.of(COMBAT_ROOM_ID), BOSS_ROOM_ID);
    }

    private RoomNode room(int id, RoomType type, int gridX, Set<Integer> connections) {
        return new RoomNode(id, type, new GridPosition(gridX, 0), connections);
    }

    private PotDefinition pot(long seed, boolean containsKey, ItemRarity rarityHint) {
        return new PotDefinition(seed, containsKey, false, rarityHint);
    }

    private BlockPosition position(int roomGridX, int offsetX, int offsetZ) {
        return new BlockPosition(
                DungeonGeometry.worldCoordinate(roomGridX) + offsetX,
                DungeonGeometry.FLOOR_Y + 1,
                offsetZ
        );
    }
}
