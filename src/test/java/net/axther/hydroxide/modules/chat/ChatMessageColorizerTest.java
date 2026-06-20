package net.axther.hydroxide.modules.chat;

import net.axther.hydroxide.text.TextFormatter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ChatMessageColorizerTest {

    private final TextFormatter formatter = new TextFormatter();

    @Test
    void returnsOriginalMessageWhenNoFormattingOrDefaultColorApplies() {
        Component original = Component.text("Hello");

        Component rendered = ChatMessageColorizer.render(formatter, original, "Hello", false, Optional.empty());

        assertEquals(original, rendered);
    }

    @Test
    void appliesSelectedChatColorWithoutParsingPlayerTagsWhenFormattingIsDenied() {
        Component rendered = ChatMessageColorizer.render(formatter, Component.text("<red>Hello"), "<red>Hello", false,
                ColorPalette.pick("#44CCFF"));

        assertEquals("<red>Hello", formatter.plain(rendered));
        assertEquals(TextColor.fromHexString("#44CCFF"), rendered.color());
    }

    @Test
    void usesSelectedChatColorAsDefaultWhenFormattingIsAllowed() {
        Component rendered = ChatMessageColorizer.render(formatter, Component.text("Hello"), "Hello", true,
                ColorPalette.pick("red"));

        assertEquals("Hello", formatter.plain(rendered));
        assertEquals(TextColor.fromHexString("#FF5555"), rendered.color());
    }
}
