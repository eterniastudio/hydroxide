package net.axther.hydroxide.modules.moderation;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModerationNoTargetCommandParserTest {

    @Test
    void parsesSelfToggleAndSelfState() {
        ModerationNoTargetCommandParser.Request toggle = ModerationNoTargetCommandParser.parse(List.of()).orElseThrow();
        ModerationNoTargetCommandParser.Request enabled = ModerationNoTargetCommandParser.parse(List.of("true")).orElseThrow();
        ModerationNoTargetCommandParser.Request disabled = ModerationNoTargetCommandParser.parse(List.of("off")).orElseThrow();

        assertTrue(toggle.targetName().isEmpty());
        assertEquals(ModerationNoTargetCommandParser.State.TOGGLE, toggle.state());
        assertEquals(ModerationNoTargetCommandParser.State.ENABLED, enabled.state());
        assertEquals(ModerationNoTargetCommandParser.State.DISABLED, disabled.state());
    }

    @Test
    void parsesTargetToggleAndExplicitState() {
        ModerationNoTargetCommandParser.Request targetToggle = ModerationNoTargetCommandParser.parse(List.of("Alex")).orElseThrow();
        ModerationNoTargetCommandParser.Request targetEnabled = ModerationNoTargetCommandParser.parse(List.of("Alex", "true")).orElseThrow();

        assertEquals("Alex", targetToggle.targetName().orElseThrow());
        assertEquals(ModerationNoTargetCommandParser.State.TOGGLE, targetToggle.state());
        assertEquals("Alex", targetEnabled.targetName().orElseThrow());
        assertEquals(ModerationNoTargetCommandParser.State.ENABLED, targetEnabled.state());
    }

    @Test
    void rejectsInvalidStateOrExtraArguments() {
        assertTrue(ModerationNoTargetCommandParser.parse(List.of("Alex", "maybe")).isEmpty());
        assertTrue(ModerationNoTargetCommandParser.parse(List.of("Alex", "true", "extra")).isEmpty());
    }
}
