package me.slmnfll.dungeons.story.prologue;

import me.slmnfll.dungeons.instance.DungeonInstanceState;
import me.slmnfll.dungeons.instance.DungeonRunEndReason;

import java.util.Objects;

public final class PrologueProgress {
    private PrologueStage stage;

    public PrologueProgress(PrologueStage stage) {
        this.stage = Objects.requireNonNull(stage, "stage");
    }

    public PrologueStage stage() {
        return stage;
    }

    public void advance(PrologueStage expected, PrologueStage next) {
        if (stage != expected) {
            throw new IllegalStateException("That prologue choice is no longer available.");
        }
        stage = Objects.requireNonNull(next, "next");
    }

    public boolean resolveTutorialClose(DungeonInstanceState finalState, DungeonRunEndReason endReason) {
        if (stage != PrologueStage.TUTORIAL_ACTIVE) {
            return false;
        }
        stage = finalState == DungeonInstanceState.COMPLETED || endReason == DungeonRunEndReason.DEATH
                ? PrologueStage.RETURN_TO_CHRONICLER
                : PrologueStage.TUTORIAL_READY;
        return true;
    }

    public void reset() {
        stage = PrologueStage.NEW;
    }
}
