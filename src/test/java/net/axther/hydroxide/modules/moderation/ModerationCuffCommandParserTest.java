package net.axther.hydroxide.modules.moderation;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModerationCuffCommandParserTest {

    @Test
    void parsesTargetToggleAndExplicitStates() {
        ModerationCuffCommandParser.Request toggle = ModerationCuffCommandParser.parse(List.of("Alex")).orElseThrow();
        ModerationCuffCommandParser.Request enabled = ModerationCuffCommandParser.parse(List.of("Alex", "true")).orElseThrow();
        ModerationCuffCommandParser.Request disabled = ModerationCuffCommandParser.parse(List.of("Alex", "off")).orElseThrow();

        assertEquals("Alex", toggle.targetName());
        assertEquals(ModerationCuffCommandParser.State.TOGGLE, toggle.state());
        assertEquals(ModerationCuffCommandParser.State.ENABLED, enabled.state());
        assertEquals(ModerationCuffCommandParser.State.DISABLED, disabled.state());
        assertFalse(toggle.silent());
    }

    @Test
    void parsesSilentFlagInAnyOptionalPosition() {
        ModerationCuffCommandParser.Request silentToggle = ModerationCuffCommandParser.parse(List.of("Alex", "-s")).orElseThrow();
        ModerationCuffCommandParser.Request silentEnabled = ModerationCuffCommandParser.parse(List.of("Alex", "-s", "on")).orElseThrow();

        assertTrue(silentToggle.silent());
        assertTrue(silentEnabled.silent());
        assertEquals(ModerationCuffCommandParser.State.ENABLED, silentEnabled.state());
    }

    @Test
    void rejectsMissingTargetInvalidStateOrExtraArguments() {
        assertTrue(ModerationCuffCommandParser.parse(List.of()).isEmpty());
        assertTrue(ModerationCuffCommandParser.parse(List.of("Alex", "maybe")).isEmpty());
        assertTrue(ModerationCuffCommandParser.parse(List.of("Alex", "true", "extra")).isEmpty());
    }
}
