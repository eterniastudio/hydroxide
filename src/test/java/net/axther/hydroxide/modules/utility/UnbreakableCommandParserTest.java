package net.axther.hydroxide.modules.utility;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UnbreakableCommandParserTest {

    @Test
    void defaultsToSelfToggle() {
        UnbreakableCommandParser.Request request = UnbreakableCommandParser.parse(List.of()).orElseThrow();

        assertTrue(request.target().isEmpty());
        assertTrue(request.state().isEmpty());
    }

    @Test
    void parsesExplicitSelfStateAliases() {
        assertEquals(true, UnbreakableCommandParser.parse(List.of("true")).orElseThrow().state().orElseThrow());
        assertEquals(true, UnbreakableCommandParser.parse(List.of("on")).orElseThrow().state().orElseThrow());
        assertEquals(false, UnbreakableCommandParser.parse(List.of("false")).orElseThrow().state().orElseThrow());
        assertEquals(false, UnbreakableCommandParser.parse(List.of("off")).orElseThrow().state().orElseThrow());
        assertTrue(UnbreakableCommandParser.parse(List.of("toggle")).orElseThrow().state().isEmpty());
    }

    @Test
    void parsesTargetToggleAndTargetState() {
        UnbreakableCommandParser.Request toggle = UnbreakableCommandParser.parse(List.of("Alex")).orElseThrow();
        UnbreakableCommandParser.Request enabled = UnbreakableCommandParser.parse(List.of("Alex", "enabled")).orElseThrow();
        UnbreakableCommandParser.Request disabled = UnbreakableCommandParser.parse(List.of("Alex", "disabled")).orElseThrow();

        assertEquals("Alex", toggle.target().orElseThrow());
        assertTrue(toggle.state().isEmpty());
        assertEquals(true, enabled.state().orElseThrow());
        assertEquals(false, disabled.state().orElseThrow());
    }

    @Test
    void rejectsUnknownExplicitStateOrTooManyArguments() {
        assertTrue(UnbreakableCommandParser.parse(List.of("Alex", "maybe")).isEmpty());
        assertTrue(UnbreakableCommandParser.parse(List.of("Alex", "true", "extra")).isEmpty());
    }
}
