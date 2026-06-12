package net.axther.hydroxide.text;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TextFormatterTest {

    private final TextFormatter formatter = new TextFormatter();

    @Test
    void formatsLegacyAmpersandColorsAndDecorations() {
        Component component = formatter.format("&a&lHydroxide");

        assertEquals("Hydroxide", formatter.plain(component));
        assertEquals(NamedTextColor.GREEN, component.color());
        assertTrue(component.hasDecoration(TextDecoration.BOLD));
    }

    @Test
    void formatsModernHexColorsFromAmpersandSyntax() {
        Component component = formatter.format("&#44CCFFModern");

        assertEquals("Modern", formatter.plain(component));
        assertEquals(TextColor.fromHexString("#44CCFF"), component.color());
    }

    @Test
    void keepsMiniMessageTagsAvailable() {
        Component component = formatter.format("<red><italic>Alert");

        assertEquals("Alert", formatter.plain(component));
        assertEquals(NamedTextColor.RED, component.color());
        assertTrue(component.hasDecoration(TextDecoration.ITALIC));
    }

    @Test
    void escapesDoubleAmpersandAsLiteralAmpersand() {
        Component component = formatter.format("Fish && Chips &7today");

        assertEquals("Fish & Chips today", formatter.plain(component));
        assertFalse(component.hasDecoration(TextDecoration.BOLD));
    }

    @Test
    void blankInputFormatsAsEmptyComponent() {
        assertEquals("", formatter.plain(formatter.format(null)));
        assertEquals("", formatter.plain(formatter.format("")));
    }
}
