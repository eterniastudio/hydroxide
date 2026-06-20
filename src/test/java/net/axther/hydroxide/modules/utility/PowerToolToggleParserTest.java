package net.axther.hydroxide.modules.utility;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PowerToolToggleParserTest {

    @Test
    void parsesExplicitStates() {
        assertEquals(PowerToolToggleParser.State.ENABLED, PowerToolToggleParser.parse(List.of("on")).orElseThrow());
        assertEquals(PowerToolToggleParser.State.DISABLED, PowerToolToggleParser.parse(List.of("off")).orElseThrow());
        assertEquals(PowerToolToggleParser.State.ENABLED, PowerToolToggleParser.parse(List.of("enable")).orElseThrow());
        assertEquals(PowerToolToggleParser.State.DISABLED, PowerToolToggleParser.parse(List.of("disable")).orElseThrow());
    }

    @Test
    void parsesDefaultToggle() {
        assertEquals(PowerToolToggleParser.State.TOGGLE, PowerToolToggleParser.parse(List.of()).orElseThrow());
        assertEquals(PowerToolToggleParser.State.TOGGLE, PowerToolToggleParser.parse(List.of("toggle")).orElseThrow());
    }

    @Test
    void rejectsUnknownOrExtraArguments() {
        assertTrue(PowerToolToggleParser.parse(List.of("yes")).isEmpty());
        assertTrue(PowerToolToggleParser.parse(List.of("on", "extra")).isEmpty());
    }
}
