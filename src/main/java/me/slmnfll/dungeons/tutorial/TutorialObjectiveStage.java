package me.slmnfll.dungeons.tutorial;

enum TutorialObjectiveStage {
    REACH_POT_ROOM(
            "Proceed to the pot-training chamber",
            "Follow the corridor and the Guild Survey Markers. Training pots remain sealed until instructed."
    ),
    BREAK_POT(
            "Break a decorated pot",
            "Right-click, strike, or shoot a decorated pot. Its contents are collected automatically."
    ),
    FIND_KEY(
            "Search the next pots for a Dungeon Key",
            "Survey keys are virtual. Your current key count appears above the hotbar."
    ),
    OPEN_CHEST(
            "Use the Dungeon Key on the locked chest",
            "Loot recovered here remains Unsecured until you reach the exit portal."
    ),
    CLEAR_ENCOUNTER(
            "Defeat the sealed-room encounter",
            "Entering a combat room seals its exits until every wave is cleared."
    ),
    ENTER_WARDEN_CHAMBER(
            "Enter the final chamber",
            "The required encounter has broken the Warden's seal."
    ),
    DEFEAT_WARDEN(
            "Defeat the Aevori Survey Warden",
            "Right-click Mossbread for Regeneration I, or Lumen Berries for a shorter Regeneration III burst."
    ),
    REACH_EXIT(
            "Reach the exit portal and secure your loot",
            "Only crossing the portal converts Unsecured loot into permanent loot."
    );

    private final String objective;
    private final String guidance;

    TutorialObjectiveStage(String objective, String guidance) {
        this.objective = objective;
        this.guidance = guidance;
    }

    String objective() {
        return objective;
    }

    String guidance() {
        return guidance;
    }
}
