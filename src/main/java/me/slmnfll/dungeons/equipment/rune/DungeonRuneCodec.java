package me.slmnfll.dungeons.equipment.rune;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class DungeonRuneCodec {
    public static final int MAX_SOCKETS = 3;

    private DungeonRuneCodec() {
    }

    public static List<DungeonRuneDefinition> decode(String encoded, DungeonRuneRegistry registry) {
        Objects.requireNonNull(registry, "registry");
        if (encoded == null || encoded.isBlank()) {
            return List.of();
        }
        List<DungeonRuneDefinition> runes = new ArrayList<>();
        for (String id : encoded.split(",")) {
            registry.find(id).ifPresent(runes::add);
        }
        return List.copyOf(runes);
    }

    public static String encode(List<DungeonRuneDefinition> runes) {
        Objects.requireNonNull(runes, "runes");
        return runes.stream().map(DungeonRuneDefinition::id).reduce((left, right) -> left + "," + right).orElse("");
    }

    public static double bonus(List<DungeonRuneDefinition> runes, DungeonRuneEffect effect) {
        Objects.requireNonNull(runes, "runes");
        Objects.requireNonNull(effect, "effect");
        return runes.stream()
                .filter(rune -> rune.effect() == effect)
                .mapToDouble(DungeonRuneDefinition::bonus)
                .sum();
    }
}
