package me.slmnfll.dungeons.encounter;

public record EncounterDefinition(
        int roomId,
        boolean required,
        boolean boss,
        int waveCount,
        int depth
) {
    public EncounterDefinition {
        if (waveCount < 1 || depth < 0) {
            throw new IllegalArgumentException("Invalid encounter wave count or depth.");
        }
    }
}
