package me.slmnfll.dungeons.tutorial;

final class TutorialObjectivePolicy {
    TutorialObjectiveStage stage(Snapshot snapshot) {
        if (snapshot.brokenPots() == 0) {
            return snapshot.inPotRoom()
                    ? TutorialObjectiveStage.BREAK_POT
                    : TutorialObjectiveStage.REACH_POT_ROOM;
        }
        if (snapshot.openedChests() == 0 && snapshot.keys() == 0) {
            return TutorialObjectiveStage.FIND_KEY;
        }
        if (snapshot.openedChests() == 0) {
            return TutorialObjectiveStage.OPEN_CHEST;
        }
        if (!snapshot.requiredEncounterCleared()) {
            return TutorialObjectiveStage.CLEAR_ENCOUNTER;
        }
        if (snapshot.bossDefeated()) {
            return TutorialObjectiveStage.REACH_EXIT;
        }
        if (!snapshot.bossActive()) {
            return TutorialObjectiveStage.ENTER_WARDEN_CHAMBER;
        }
        return TutorialObjectiveStage.DEFEAT_WARDEN;
    }

    record Snapshot(
            boolean inPotRoom,
            int brokenPots,
            int keys,
            int openedChests,
            boolean requiredEncounterCleared,
            boolean bossActive,
            boolean bossDefeated
    ) {
    }
}
