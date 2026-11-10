package me.slmnfll.dungeons.content;

import java.util.Objects;
import java.util.random.RandomGenerator;

public final class DungeonLootLossPolicy {
    public int lostAmount(int stackAmount, RandomGenerator random) {
        if (stackAmount < 1) {
            throw new IllegalArgumentException("Stack amount must be positive.");
        }
        Objects.requireNonNull(random, "random");

        int lost = stackAmount / 2;
        if (stackAmount % 2 == 1 && random.nextBoolean()) {
            lost++;
        }
        return lost;
    }
}
