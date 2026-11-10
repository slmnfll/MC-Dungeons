package me.slmnfll.dungeons.world;

import java.nio.file.Path;
import java.util.regex.Pattern;

final class DungeonWorldPathPolicy {
    private static final Pattern OWNED_WORLD_NAME = Pattern.compile(
            "^" + DungeonWorldService.WORLD_PREFIX + "[0-9a-f]{32}$"
    );

    private DungeonWorldPathPolicy() {
    }

    static boolean isOwned(Path worldContainer, Path candidate) {
        Path normalizedContainer = worldContainer.toAbsolutePath().normalize();
        Path normalizedCandidate = candidate.toAbsolutePath().normalize();
        Path fileName = normalizedCandidate.getFileName();
        return fileName != null
                && isOwnedWorldName(fileName.toString())
                && normalizedCandidate.startsWith(normalizedContainer)
                && !normalizedCandidate.equals(normalizedContainer);
    }

    static boolean isOwnedWorldName(String worldName) {
        return worldName != null && OWNED_WORLD_NAME.matcher(worldName).matches();
    }
}
