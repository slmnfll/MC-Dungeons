package me.slmnfll.dungeons.story.prologue;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

final class PrologueMessageTiming {
    static final long MINIMUM_DELAY_TICKS = 30L;
    static final long MAXIMUM_DELAY_TICKS = 40L;

    private static final int SHORT_MESSAGE_LENGTH = 12;
    private static final int CHARACTERS_PER_EXTRA_TICK = 4;
    private static final long FIRST_MESSAGE_RENDER_ALLOWANCE_TICKS = 4L;

    long delayTicks(Component message, boolean firstMessage) {
        String text = PlainTextComponentSerializer.plainText().serialize(message).strip();
        int visibleCharacters = text.codePointCount(0, text.length());
        int charactersBeyondShortMessage = Math.max(0, visibleCharacters - SHORT_MESSAGE_LENGTH);
        long readingTimeTicks = MINIMUM_DELAY_TICKS
                + divideRoundingUp(charactersBeyondShortMessage, CHARACTERS_PER_EXTRA_TICK);
        long renderAllowance = firstMessage ? FIRST_MESSAGE_RENDER_ALLOWANCE_TICKS : 0L;
        return Math.min(MAXIMUM_DELAY_TICKS, readingTimeTicks + renderAllowance);
    }

    private long divideRoundingUp(int dividend, int divisor) {
        return (dividend + divisor - 1L) / divisor;
    }
}
