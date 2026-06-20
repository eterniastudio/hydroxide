package net.axther.hydroxide.modules.chat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ColorPaletteTest {

    @Test
    void findsLegacyNamedColors() {
        ColorPalette.Selection selection = ColorPalette.pick("red").orElseThrow();

        assertEquals("red", selection.closest().name());
        assertEquals("&c", selection.closest().legacy());
        assertEquals("#FF5555", selection.hex());
        assertEquals("<#FF5555>", selection.miniMessage());
    }

    @Test
    void acceptsHexInputAndKeepsRequestedHex() {
        ColorPalette.Selection selection = ColorPalette.pick("#FE5454").orElseThrow();

        assertEquals("red", selection.closest().name());
        assertEquals("#FE5454", selection.hex());
        assertEquals("<#FE5454>", selection.miniMessage());
    }

    @Test
    void rejectsUnknownInputAndListsAllLegacyEntries() {
        assertTrue(ColorPalette.pick("not-a-color").isEmpty());
        assertEquals(16, ColorPalette.entries().size());
        assertTrue(ColorPalette.entries().stream().anyMatch(entry -> entry.name().equals("white")));
    }
}
