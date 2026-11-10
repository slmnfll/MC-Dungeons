package me.slmnfll.dungeons.story.prologue;

import io.papermc.paper.registry.data.dialog.ActionButton;
import net.kyori.adventure.text.Component;

import java.util.List;
import java.util.Objects;

record PrologueConversation(
        List<Component> messages,
        Component finalMessage,
        List<ActionButton> responseButtons,
        boolean waitsForContinuation
) {
    PrologueConversation {
        messages = List.copyOf(messages);
        messages.forEach(message -> Objects.requireNonNull(message, "message"));
        Objects.requireNonNull(finalMessage, "finalMessage");
        responseButtons = List.copyOf(responseButtons);
        responseButtons.forEach(button -> Objects.requireNonNull(button, "button"));
    }
}
