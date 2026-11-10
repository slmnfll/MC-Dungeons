package me.slmnfll.dungeons.equipment.consumable;

public enum ConsumableAbility {
    REGENERATION("Regeneration", "Restores health gradually for a limited time."),
    HEALING("Healing", "Restores four hearts when consumed."),
    STONEHIDE("Stonehide", "Grants Resistance for fifteen seconds."),
    CINDER_BURST("Cinder Burst", "Damages and ignites creatures near the impact.");

    private final String displayName;
    private final String description;

    ConsumableAbility(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String displayName() {
        return displayName;
    }

    public String description() {
        return description;
    }
}
