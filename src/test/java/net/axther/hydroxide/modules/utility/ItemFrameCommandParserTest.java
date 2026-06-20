package net.axther.hydroxide.modules.utility;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemFrameCommandParserTest {

    @Test
    void parsesFrameProperties() {
        assertEquals(ItemFrameCommandParser.Property.INVISIBLE,
                ItemFrameCommandParser.parse(List.of("invisible")).orElseThrow().property());
        assertEquals(ItemFrameCommandParser.Property.FIXED,
                ItemFrameCommandParser.parse(List.of("fixed")).orElseThrow().property());
        assertEquals(ItemFrameCommandParser.Property.INVULNERABLE,
                ItemFrameCommandParser.parse(List.of("invulnerable")).orElseThrow().property());
        assertEquals(ItemFrameCommandParser.Property.ALL,
                ItemFrameCommandParser.parse(List.of("all")).orElseThrow().property());
    }

    @Test
    void acceptsCommonAliases() {
        assertEquals(ItemFrameCommandParser.Property.INVISIBLE,
                ItemFrameCommandParser.parse(List.of("visible")).orElseThrow().property());
        assertEquals(ItemFrameCommandParser.Property.FIXED,
                ItemFrameCommandParser.parse(List.of("fix")).orElseThrow().property());
        assertEquals(ItemFrameCommandParser.Property.INVULNERABLE,
                ItemFrameCommandParser.parse(List.of("invul")).orElseThrow().property());
    }

    @Test
    void rejectsUnknownOrExtraArguments() {
        assertTrue(ItemFrameCommandParser.parse(List.of()).isEmpty());
        assertTrue(ItemFrameCommandParser.parse(List.of("glowing")).isEmpty());
        assertTrue(ItemFrameCommandParser.parse(List.of("fixed", "extra")).isEmpty());
    }
}
