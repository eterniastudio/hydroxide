package net.axther.hydroxide.modules.utility;

import org.bukkit.Color;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DyeCommandParserTest {

    @Test
    void parsesHexColors() {
        DyeCommandParser.Request request = DyeCommandParser.parse(List.of("#44CCFF")).orElseThrow();

        assertEquals(DyeCommandParser.Action.SET, request.action());
        assertEquals(Color.fromRGB(0x44, 0xCC, 0xFF), request.color().orElseThrow());
    }

    @Test
    void parsesRgbTriples() {
        DyeCommandParser.Request request = DyeCommandParser.parse(List.of("12,34,56")).orElseThrow();

        assertEquals(DyeCommandParser.Action.SET, request.action());
        assertEquals(Color.fromRGB(12, 34, 56), request.color().orElseThrow());
    }

    @Test
    void parsesNamedDyeColors() {
        DyeCommandParser.Request request = DyeCommandParser.parse(List.of("light_blue")).orElseThrow();

        assertEquals(DyeCommandParser.Action.SET, request.action());
        assertTrue(request.color().isPresent());
    }

    @Test
    void parsesRandomAndClearActions() {
        assertEquals(DyeCommandParser.Action.RANDOM, DyeCommandParser.parse(List.of("random")).orElseThrow().action());
        assertEquals(DyeCommandParser.Action.CLEAR, DyeCommandParser.parse(List.of("clear")).orElseThrow().action());
        assertEquals(DyeCommandParser.Action.CLEAR, DyeCommandParser.parse(List.of("reset")).orElseThrow().action());
    }

    @Test
    void rejectsInvalidForms() {
        assertTrue(DyeCommandParser.parse(List.of()).isEmpty());
        assertTrue(DyeCommandParser.parse(List.of("not-a-color")).isEmpty());
        assertTrue(DyeCommandParser.parse(List.of("256,0,0")).isEmpty());
        assertTrue(DyeCommandParser.parse(List.of("#XYZXYZ")).isEmpty());
        assertTrue(DyeCommandParser.parse(List.of("red", "extra")).isEmpty());
    }
}
