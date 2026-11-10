package me.slmnfll.dungeons.story.prologue;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import me.slmnfll.dungeons.affinity.Affinity;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import java.util.ArrayList;
import java.util.List;

final class PrologueDialogueFactory {
    private static final int TRANSCRIPT_WIDTH = 420;

    PrologueConversation introduction() {
        return conversation(
                List.of(
                        speech("New boots. An undamaged coat."),
                        speech("And entirely too much confidence.")
                ),
                speech("You must be our latest recruit."),
                List.of(
                        choiceButton("I'm a professional.", "intro_professional", "A bold opening claim."),
                        choiceButton("I've opened several chests.", "intro_chests", "Experience is experience."),
                        choiceButton("Nobody mentioned dungeons.", "intro_unaware", "Honesty may keep you alive.")
                ),
                true
        );
    }

    PrologueConversation affinity(String introductionChoice) {
        List<Component> reply = switch (introductionChoice) {
            case "intro_professional" -> List.of(
                    speech("Wonderful."),
                    speech("Then this should be only mildly embarrassing.")
            );
            case "intro_chests" -> List.of(
                    speech("Chests, you say?"),
                    speech("An excellent foundation for a career in archaeology.")
            );
            default -> List.of(
                    speech("Ah."),
                    speech("Then you have already learned the guild's approach to recruitment.")
            );
        };
        return affinityConversation(reply);
    }

    PrologueConversation affinityReminder() {
        return affinityConversation(List.of());
    }

    PrologueConversation tutorialReady(Affinity affinity) {
        String weapon = switch (affinity) {
            case VANGUARD -> "A sword. Reliable, direct, and difficult to misplace.";
            case ARBALIST -> "A crossbow. Danger is healthier at a respectful distance.";
            case RAVAGER -> "An axe. Useful for combat and academic disagreements.";
        };
        return conversation(
                List.of(
                        speech("The engineer will issue your starter equipment inside."),
                        Component.text(weapon, NamedTextColor.GOLD),
                        speech("Arrive with an empty inventory. Yes, all of it."),
                        speech("Survey Site Zero is a shallow Aevori ruin."),
                        speech("It is mapped. It is controlled.")
                ),
                speech("It is allegedly safe."),
                List.of(
                        proceedButton("✔ Begin the survey", "begin_tutorial", "Enter Survey Site Zero."),
                        cancelButton("❌ Not yet", "not_yet", "Close the conversation.")
                ),
                false
        );
    }

    PrologueConversation activeTutorial() {
        return conversation(
                List.of(),
                speech("Complete Survey Site Zero before requesting another assignment."),
                List.of(cancelButton("❌ Return to survey", "close", "Close the conversation.")),
                false
        );
    }

    PrologueConversation returnFromTutorial() {
        return conversation(
                List.of(
                        speech("That chamber was not on our maps."),
                        speech("And the Warden was not one of ours."),
                        speech("Whatever spoke to you came from deeper inside Aevori territory.")
                ),
                Component.text("The gate remembers us.", NamedTextColor.DARK_PURPLE)
                        .decorate(TextDecoration.ITALIC),
                List.of(proceedButton("What happens now?", "finish_prologue", "Complete the prologue.")),
                true
        );
    }

    PrologueConversation completed() {
        return conversation(
                List.of(speech("The expedition is preparing a route to the Rootbound Gate.")),
                speech("For now, keep your journal close and your confidence slightly lower."),
                List.of(cancelButton("✕ Close", "close", "Close the conversation.")),
                false
        );
    }

    Dialog message(Component body) {
        return dialogue("Guild Chronicler", body, List.of(), false, false);
    }

    Dialog response(
            Component body,
            List<ActionButton> buttons,
            boolean waitsForContinuation
    ) {
        return dialogue("Guild Chronicler", body, buttons, true, waitsForContinuation);
    }

    private PrologueConversation affinityConversation(List<Component> messages) {
        return conversation(
                messages,
                speech("Choose an affinity. The engineer insists all three options are survivable."),
                List.of(
                        choiceButton("Vanguard — Sword", "affinity_vanguard", "Direct melee combat."),
                        choiceButton("Arbalist — Crossbow", "affinity_arbalist", "Ranged precision."),
                        choiceButton("Ravager — Axe", "affinity_ravager", "Heavy melee attacks.")
                ),
                true
        );
    }

    private PrologueConversation conversation(
            List<Component> messages,
            Component finalMessage,
            List<ActionButton> buttons,
            boolean waitsForContinuation
    ) {
        return new PrologueConversation(
                messages,
                finalMessage,
                buttons,
                waitsForContinuation
        );
    }

    private Dialog dialogue(
            String title,
            Component body,
            List<ActionButton> buttons,
            boolean canClose,
            boolean waitsForContinuation
    ) {
        List<DialogBody> bodyElements = new ArrayList<>();
        bodyElements.add(DialogBody.plainMessage(body, TRANSCRIPT_WIDTH));
        if (!buttons.isEmpty()) {
            bodyElements.add(DialogBody.plainMessage(Component.text(" "), 1));
        }

        DialogBase base = DialogBase.builder(Component.text(title, NamedTextColor.DARK_PURPLE)
                        .decorate(TextDecoration.BOLD))
                .externalTitle(Component.text(title))
                .canCloseWithEscape(canClose)
                .pause(false)
                .afterAction(canClose
                        ? (waitsForContinuation
                                ? DialogBase.DialogAfterAction.WAIT_FOR_RESPONSE
                                : DialogBase.DialogAfterAction.CLOSE)
                        : DialogBase.DialogAfterAction.NONE)
                .body(bodyElements)
                .build();
        return Dialog.create(factory -> {
            var builder = factory.empty().base(base);
            if (buttons.isEmpty()) {
                builder.type(DialogType.notice(waitingIndicator()));
            } else {
                builder.type(DialogType.multiAction(buttons).columns(1).build());
            }
        });
    }

    private Component speech(String text) {
        return Component.text(text, NamedTextColor.WHITE);
    }

    private ActionButton choiceButton(String label, String response, String tooltip) {
        return button(label, response, tooltip, NamedTextColor.AQUA);
    }

    private ActionButton proceedButton(String label, String response, String tooltip) {
        return button(label, response, tooltip, NamedTextColor.GREEN);
    }

    private ActionButton cancelButton(String label, String response, String tooltip) {
        return button(label, response, tooltip, NamedTextColor.RED);
    }

    private ActionButton button(
            String label,
            String response,
            String tooltip,
            NamedTextColor color
    ) {
        return ActionButton.create(
                Component.text(label, color),
                Component.text(tooltip, NamedTextColor.GRAY),
                220,
                DialogAction.staticAction(ClickEvent.runCommand(
                        "/dungeons prologue respond " + response
                ))
        );
    }

    private ActionButton waitingIndicator() {
        return ActionButton.create(Component.empty(), null, 1, null);
    }
}
