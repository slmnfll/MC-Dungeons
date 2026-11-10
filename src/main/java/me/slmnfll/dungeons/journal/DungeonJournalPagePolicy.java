package me.slmnfll.dungeons.journal;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.util.List;

public final class DungeonJournalPagePolicy {
    public static final int MAX_PAGES = 8;
    public static final int MAX_PLAIN_CHARACTERS_PER_PAGE = 600;
    public static final int MAX_LINES_PER_PAGE = 14;
    public static final int MAX_CHARACTERS_PER_LINE = 32;

    public void validate(List<Component> pages) {
        if (pages.isEmpty() || pages.size() > MAX_PAGES) {
            throw new IllegalArgumentException("The journal must contain between one and eight pages.");
        }
        PlainTextComponentSerializer serializer = PlainTextComponentSerializer.plainText();
        for (int index = 0; index < pages.size(); index++) {
            String text = serializer.serialize(pages.get(index));
            if (text.length() > MAX_PLAIN_CHARACTERS_PER_PAGE) {
                throw new IllegalArgumentException("Journal page " + (index + 1) + " is too long.");
            }
            String[] lines = text.split("\\n", -1);
            if (lines.length > MAX_LINES_PER_PAGE) {
                throw new IllegalArgumentException("Journal page " + (index + 1) + " has too many lines.");
            }
            for (String line : lines) {
                if (line.length() > MAX_CHARACTERS_PER_LINE) {
                    throw new IllegalArgumentException(
                            "Journal page " + (index + 1) + " contains an oversized line."
                    );
                }
            }
        }
    }
}
