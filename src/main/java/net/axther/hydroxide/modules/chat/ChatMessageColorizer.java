package net.axther.hydroxide.modules.chat;

import net.axther.hydroxide.text.TextFormatter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;

import java.util.Optional;

final class ChatMessageColorizer {

    private ChatMessageColorizer() {
    }

    static Component render(TextFormatter text, Component originalMessage, String messageText, boolean allowPlayerFormatting,
                            Optional<ColorPalette.Selection> selectedColor) {
        if (allowPlayerFormatting) {
            return selectedColor
                    .map(selection -> text.format(selection.miniMessage() + messageText))
                    .orElseGet(() -> text.format(messageText));
        }
        if (selectedColor.isPresent()) {
            ColorPalette.Selection selection = selectedColor.orElseThrow();
            return Component.text(messageText).color(TextColor.fromHexString(selection.hex()));
        }
        return originalMessage;
    }
}
