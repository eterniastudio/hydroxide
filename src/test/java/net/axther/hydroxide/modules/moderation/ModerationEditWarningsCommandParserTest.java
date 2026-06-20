package net.axther.hydroxide.modules.moderation;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModerationEditWarningsCommandParserTest {

    @Test
    void parsesPlayerClearAndClearAllRequests() {
        ModerationEditWarningsCommandParser.Request player = ModerationEditWarningsCommandParser.parse(List.of("Alex", "clear")).orElseThrow();
        ModerationEditWarningsCommandParser.Request clearAll = ModerationEditWarningsCommandParser.parse(List.of("clearall")).orElseThrow();
        ModerationEditWarningsCommandParser.Request clearAllExplicit = ModerationEditWarningsCommandParser.parse(List.of("clearall", "clear")).orElseThrow();

        assertEquals(ModerationEditWarningsCommandParser.Action.CLEAR_PLAYER, player.action());
        assertEquals("Alex", player.targetName().orElseThrow());
        assertEquals(ModerationEditWarningsCommandParser.Action.CLEAR_ALL, clearAll.action());
        assertTrue(clearAll.targetName().isEmpty());
        assertEquals(ModerationEditWarningsCommandParser.Action.CLEAR_ALL, clearAllExplicit.action());
    }

    @Test
    void rejectsInvalidShapes() {
        assertTrue(ModerationEditWarningsCommandParser.parse(List.of()).isEmpty());
        assertTrue(ModerationEditWarningsCommandParser.parse(List.of("Alex")).isEmpty());
        assertTrue(ModerationEditWarningsCommandParser.parse(List.of("Alex", "remove")).isEmpty());
        assertTrue(ModerationEditWarningsCommandParser.parse(List.of("clearall", "remove")).isEmpty());
        assertTrue(ModerationEditWarningsCommandParser.parse(List.of("Alex", "clear", "extra")).isEmpty());
    }
}
