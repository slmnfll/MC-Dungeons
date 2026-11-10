package me.slmnfll.dungeons.content;

import me.slmnfll.dungeons.layout.DungeonLayout;
import me.slmnfll.dungeons.layout.RoomNode;
import me.slmnfll.dungeons.layout.RoomType;
import me.slmnfll.dungeons.world.DungeonGeometry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public final class DungeonContentPlanner {
    public static final int MIN_POTS = 24;
    public static final int MAX_POTS = 50;

    private static final long CONTENT_SEED_SALT = 0x5DEECE66DL;
    private static final List<Offset> POT_OFFSETS = List.of(
            new Offset(-5, -5), new Offset(5, -5),
            new Offset(-5, 5), new Offset(5, 5),
            new Offset(-3, -5), new Offset(3, -5),
            new Offset(-3, 5), new Offset(3, 5)
    );

    public DungeonContentPlan plan(DungeonLayout layout) {
        Random random = new Random(layout.seed() ^ CONTENT_SEED_SALT);
        Map<BlockPosition, LockedChestDefinition> chests = planChests(layout, random);

        List<BlockPosition> candidates = potCandidates(layout);
        Collections.shuffle(candidates, random);
        int desiredPots = MIN_POTS + random.nextInt(MAX_POTS - MIN_POTS + 1);
        int potCount = Math.min(desiredPots, candidates.size());
        List<BlockPosition> selectedPositions = new ArrayList<>(candidates.subList(0, potCount));

        List<BlockPosition> guaranteedKeyPositions = new ArrayList<>(selectedPositions);
        Collections.shuffle(guaranteedKeyPositions, new Random(layout.seed() ^ 0xC0FFEE1234L));
        guaranteedKeyPositions = guaranteedKeyPositions.subList(0, Math.min(chests.size(), guaranteedKeyPositions.size()));

        Map<BlockPosition, PotDefinition> pots = new LinkedHashMap<>();
        for (BlockPosition position : selectedPositions) {
            ItemRarity rarityHint = rollRarityHint(random);
            pots.put(position, new PotDefinition(
                    random.nextLong(),
                    guaranteedKeyPositions.contains(position),
                    random.nextDouble() < 0.12D,
                    rarityHint
            ));
        }
        return new DungeonContentPlan(pots, chests);
    }

    private Map<BlockPosition, LockedChestDefinition> planChests(DungeonLayout layout, Random random) {
        Map<BlockPosition, LockedChestDefinition> chests = new LinkedHashMap<>();
        for (RoomNode room : layout.rooms()) {
            if (room.type() != RoomType.TREASURE) {
                continue;
            }
            BlockPosition position = new BlockPosition(
                    DungeonGeometry.worldCoordinate(room.position().x()),
                    DungeonGeometry.FLOOR_Y + 1,
                    DungeonGeometry.worldCoordinate(room.position().z())
            );
            chests.put(position, new LockedChestDefinition(room.id(), random.nextLong()));
        }
        return chests;
    }

    private List<BlockPosition> potCandidates(DungeonLayout layout) {
        List<BlockPosition> positions = new ArrayList<>();
        for (RoomNode room : layout.rooms()) {
            if (room.type() == RoomType.BOSS) {
                continue;
            }
            int centerX = DungeonGeometry.worldCoordinate(room.position().x());
            int centerZ = DungeonGeometry.worldCoordinate(room.position().z());
            for (Offset offset : POT_OFFSETS) {
                positions.add(new BlockPosition(
                        centerX + offset.x(),
                        DungeonGeometry.FLOOR_Y + 1,
                        centerZ + offset.z()
                ));
            }
        }
        return positions;
    }

    private ItemRarity rollRarityHint(Random random) {
        int roll = random.nextInt(1_000);
        if (roll < 5) {
            return ItemRarity.LEGENDARY;
        }
        if (roll < 25) {
            return ItemRarity.EPIC;
        }
        if (roll < 90) {
            return ItemRarity.RARE;
        }
        if (roll < 200) {
            return ItemRarity.UNCOMMON;
        }
        return null;
    }

    private record Offset(int x, int z) {
    }
}
