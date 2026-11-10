package me.slmnfll.dungeons.story.prologue;

import java.util.Locale;
import java.util.Optional;

public enum PrologueStage {
    NEW("new"),
    CHOOSING_AFFINITY("choosing_affinity"),
    TUTORIAL_READY("tutorial_ready"),
    TUTORIAL_ACTIVE("tutorial_active"),
    RETURN_TO_CHRONICLER("return_to_chronicler"),
    COMPLETE("complete");

    private final String id;

    PrologueStage(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public static Optional<PrologueStage> fromId(String value) {
        if (value == null) {
            return Optional.empty();
        }
        String normalized = value.toLowerCase(Locale.ROOT);
        for (PrologueStage stage : values()) {
            if (stage.id.equals(normalized)) {
                return Optional.of(stage);
            }
        }
        return Optional.empty();
    }
}
