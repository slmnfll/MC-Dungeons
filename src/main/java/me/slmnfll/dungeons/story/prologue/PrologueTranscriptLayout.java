package me.slmnfll.dungeons.story.prologue;

import java.util.List;

final class PrologueTranscriptLayout {
    static final int MAX_LOGICAL_LINES = 30;
    private static final int RESPONSE_SPACER_LINES = 1;
    private static final int EARLIER_MARKER_LINES = 3;

    Window window(List<Boolean> playerSpeakers) {
        if (playerSpeakers.isEmpty()) {
            return new Window(0, RESPONSE_SPACER_LINES);
        }

        int firstVisible = playerSpeakers.size() - 1;
        int logicalLines = logicalLines(playerSpeakers, firstVisible);
        for (int candidate = firstVisible - 1; candidate >= 0; candidate--) {
            int candidateLines = logicalLines(playerSpeakers, candidate);
            if (candidateLines > MAX_LOGICAL_LINES) {
                break;
            }
            firstVisible = candidate;
            logicalLines = candidateLines;
        }
        return new Window(firstVisible, logicalLines);
    }

    private int logicalLines(List<Boolean> playerSpeakers, int firstVisible) {
        int lines = RESPONSE_SPACER_LINES;
        if (firstVisible > 0) {
            lines += EARLIER_MARKER_LINES;
        }

        Boolean previousSpeaker = null;
        for (int index = firstVisible; index < playerSpeakers.size(); index++) {
            boolean speaker = playerSpeakers.get(index);
            if (previousSpeaker == null) {
                lines += 3; // Speaker, blank line, first message.
            } else if (previousSpeaker == speaker) {
                lines += 2; // Blank line, next message.
            } else {
                lines += 4; // Blank line, speaker, blank line, first message.
            }
            previousSpeaker = speaker;
        }
        return lines;
    }

    record Window(int firstMessageIndex, int logicalLines) {
    }
}
