package me.slmnfll.dungeons.content;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import java.util.ArrayList;
import java.util.List;

final class DungeonRiskLore {
    private static final Component HEADER = Component.text("UNSECURED LOOT", NamedTextColor.RED)
            .decorate(TextDecoration.BOLD)
            .decoration(TextDecoration.ITALIC, false);
    private static final Component EXPLANATION = Component.text(
                    "Reach an exit portal to secure this item",
                    NamedTextColor.DARK_GRAY
            )
            .decoration(TextDecoration.ITALIC, false);

    List<Component> mark(List<Component> lore) {
        List<Component> marked = clear(lore);
        marked.add(Component.empty());
        marked.add(HEADER);
        marked.add(EXPLANATION);
        return List.copyOf(marked);
    }

    List<Component> clear(List<Component> lore) {
        List<Component> cleared = new ArrayList<>(lore == null ? List.of() : lore);
        if (cleared.size() >= 2
                && cleared.get(cleared.size() - 2).equals(HEADER)
                && cleared.get(cleared.size() - 1).equals(EXPLANATION)) {
            cleared.removeLast();
            cleared.removeLast();
            if (!cleared.isEmpty() && cleared.getLast().equals(Component.empty())) {
                cleared.removeLast();
            }
        }
        return cleared;
    }
}
