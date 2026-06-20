package net.axther.hydroxide.modules.moderation;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModerationAirCommandParserTest {

    @Test
    void parsesExplicitAirAmount() {
        ModerationAirCommandParser.Request request = ModerationAirCommandParser.parse(List.of("Steve", "120")).orElseThrow();

        assertEquals("Steve", request.target().name());
        assertEquals(120, request.amountTicks().orElseThrow());
        assertTrue(request.useMaximum() == false);
    }

    @Test
    void parsesMaximumAirAlias() {
        ModerationAirCommandParser.Request request = ModerationAirCommandParser.parse(List.of("all", "max")).orElseThrow();

        assertTrue(request.target().all());
        assertTrue(request.amountTicks().isEmpty());
        assertTrue(request.useMaximum());
    }

    @Test
    void rejectsMissingOrInvalidAmounts() {
        assertTrue(ModerationAirCommandParser.parse(List.of()).isEmpty());
        assertTrue(ModerationAirCommandParser.parse(List.of("Steve")).isEmpty());
        assertTrue(ModerationAirCommandParser.parse(List.of("Steve", "-1")).isEmpty());
        assertTrue(ModerationAirCommandParser.parse(List.of("Steve", "many")).isEmpty());
    }
}
