package net.axther.hydroxide.modules.moderation;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModerationPlayerCollisionCommandParserTest {

    @Test
    void parsesSelfToggleAndSelfState() {
        ModerationPlayerCollisionCommandParser.Request toggle = ModerationPlayerCollisionCommandParser.parse(List.of()).orElseThrow();
        ModerationPlayerCollisionCommandParser.Request enabled = ModerationPlayerCollisionCommandParser.parse(List.of("true")).orElseThrow();
        ModerationPlayerCollisionCommandParser.Request disabled = ModerationPlayerCollisionCommandParser.parse(List.of("off")).orElseThrow();

        assertTrue(toggle.targetName().isEmpty());
        assertEquals(ModerationPlayerCollisionCommandParser.State.TOGGLE, toggle.state());
        assertEquals(ModerationPlayerCollisionCommandParser.State.ENABLED, enabled.state());
        assertEquals(ModerationPlayerCollisionCommandParser.State.DISABLED, disabled.state());
    }

    @Test
    void parsesTargetStateAndSilentFlag() {
        ModerationPlayerCollisionCommandParser.Request request = ModerationPlayerCollisionCommandParser.parse(List.of("Alex", "false", "-s")).orElseThrow();

        assertEquals("Alex", request.targetName().orElseThrow());
        assertEquals(ModerationPlayerCollisionCommandParser.State.DISABLED, request.state());
        assertTrue(request.silent());
    }

    @Test
    void rejectsInvalidStateOrExtraArguments() {
        assertTrue(ModerationPlayerCollisionCommandParser.parse(List.of("Alex", "maybe")).isEmpty());
        assertTrue(ModerationPlayerCollisionCommandParser.parse(List.of("Alex", "true", "extra")).isEmpty());
    }
}
