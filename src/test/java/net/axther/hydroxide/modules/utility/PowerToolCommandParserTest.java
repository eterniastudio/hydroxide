package net.axther.hydroxide.modules.utility;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PowerToolCommandParserTest {

    @Test
    void parsesCommandBindingAndStripsLeadingSlash() {
        PowerToolCommandParser.Request request = PowerToolCommandParser.parse(List.of("/home", "{player}")).orElseThrow();

        assertEquals(PowerToolCommandParser.Action.BIND, request.action());
        assertEquals("home {player}", request.command().orElseThrow());
    }

    @Test
    void parsesClearAliases() {
        assertEquals(PowerToolCommandParser.Action.CLEAR, PowerToolCommandParser.parse(List.of("clear")).orElseThrow().action());
        assertEquals(PowerToolCommandParser.Action.CLEAR, PowerToolCommandParser.parse(List.of("remove")).orElseThrow().action());
        assertEquals(PowerToolCommandParser.Action.CLEAR, PowerToolCommandParser.parse(List.of("none")).orElseThrow().action());
        assertEquals(PowerToolCommandParser.Action.CLEAR, PowerToolCommandParser.parse(List.of("off")).orElseThrow().action());
    }

    @Test
    void preservesMultiWordCommand() {
        PowerToolCommandParser.Request request = PowerToolCommandParser.parse(List.of("warp", "spawn")).orElseThrow();

        assertEquals(PowerToolCommandParser.Action.BIND, request.action());
        assertEquals("warp spawn", request.command().orElseThrow());
    }

    @Test
    void rejectsMissingCommand() {
        assertTrue(PowerToolCommandParser.parse(List.of()).isEmpty());
    }
}
