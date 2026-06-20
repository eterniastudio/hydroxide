package net.axther.hydroxide.modules.moderation;

import net.kyori.adventure.text.format.NamedTextColor;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModerationGlowCommandParserTest {

    @Test
    void defaultsToSelfToggle() {
        ModerationGlowCommandParser.Request request = ModerationGlowCommandParser.parse(List.of()).orElseThrow();

        assertTrue(request.targetName().isEmpty());
        assertEquals(ModerationGlowCommandParser.Action.TOGGLE, request.action());
        assertTrue(request.color().isEmpty());
    }

    @Test
    void parsesTargetAndExplicitState() {
        ModerationGlowCommandParser.Request request = ModerationGlowCommandParser.parse(List.of("Alex", "off")).orElseThrow();

        assertEquals("Alex", request.targetName().orElseThrow());
        assertEquals(ModerationGlowCommandParser.Action.DISABLE, request.action());
        assertTrue(request.color().isEmpty());
    }

    @Test
    void parsesSelfColorAliases() {
        ModerationGlowCommandParser.Request request = ModerationGlowCommandParser.parse(List.of("pink")).orElseThrow();

        assertTrue(request.targetName().isEmpty());
        assertEquals(ModerationGlowCommandParser.Action.COLOR, request.action());
        assertEquals(NamedTextColor.LIGHT_PURPLE, request.color().orElseThrow());
    }

    @Test
    void parsesTargetColor() {
        ModerationGlowCommandParser.Request request = ModerationGlowCommandParser.parse(List.of("Alex", "dark_red")).orElseThrow();

        assertEquals("Alex", request.targetName().orElseThrow());
        assertEquals(ModerationGlowCommandParser.Action.COLOR, request.action());
        assertEquals(NamedTextColor.DARK_RED, request.color().orElseThrow());
    }

    @Test
    void rejectsUnknownSecondArgumentOrExtraArguments() {
        assertTrue(ModerationGlowCommandParser.parse(List.of("Alex", "sparkly")).isEmpty());
        assertTrue(ModerationGlowCommandParser.parse(List.of("Alex", "red", "extra")).isEmpty());
    }
}
