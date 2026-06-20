package net.axther.hydroxide.modules.moderation;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModerationSaturationCommandParserTest {

    @Test
    void parsesSinglePlayerAmount() {
        ModerationSaturationCommandParser.Request request = ModerationSaturationCommandParser.parse(List.of("Steve", "6.5")).orElseThrow();

        assertEquals("Steve", request.target().name());
        assertTrue(!request.target().all());
        assertEquals(6.5F, request.amount());
    }

    @Test
    void parsesAllTarget() {
        ModerationSaturationCommandParser.Request request = ModerationSaturationCommandParser.parse(List.of("all", "20")).orElseThrow();

        assertTrue(request.target().all());
        assertEquals(20.0F, request.amount());
    }

    @Test
    void rejectsMissingNegativeOrInvalidAmounts() {
        assertTrue(ModerationSaturationCommandParser.parse(List.of()).isEmpty());
        assertTrue(ModerationSaturationCommandParser.parse(List.of("Steve")).isEmpty());
        assertTrue(ModerationSaturationCommandParser.parse(List.of("Steve", "-1")).isEmpty());
        assertTrue(ModerationSaturationCommandParser.parse(List.of("Steve", "21")).isEmpty());
        assertTrue(ModerationSaturationCommandParser.parse(List.of("Steve", "many")).isEmpty());
    }
}
