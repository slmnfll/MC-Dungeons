package me.slmnfll.dungeons.equipment.artifact;

import java.util.Arrays;
import java.util.Optional;

public enum ArtifactAbility {
    GUARDIAN_SHELL(
            "guardian_shell",
            "Guardian Shell",
            "Gain four absorption hearts for a short time."
    ),
    GALE_STEP(
            "gale_step",
            "Gale Step",
            "Surge rapidly in the direction you are facing."
    ),
    EMBER_BURST(
            "ember_burst",
            "Ember Burst",
            "Scorch nearby creatures with a ring of fire."
    );

    private final String id;
    private final String displayName;
    private final String description;

    ArtifactAbility(String id, String displayName, String description) {
        this.id = id;
        this.displayName = displayName;
        this.description = description;
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public String description() {
        return description;
    }

    public static Optional<ArtifactAbility> fromId(String value) {
        if (value == null) {
            return Optional.empty();
        }
        return Arrays.stream(values()).filter(ability -> ability.id.equals(value)).findFirst();
    }
}
