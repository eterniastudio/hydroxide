package net.axther.hydroxide.modules.moderation;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModerationHungerCommandParserTest {

    @Test
    void parsesSinglePlayerAmount() {
        ModerationHungerCommandParser.Request request = ModerationHungerCommandParser.parse(List.of("Steve", "12")).orElseThrow();

        assertEquals("Steve", request.target().name());
        assertTrue(!request.target().all());
        assertEquals(12, request.amount());
    }

    @Test
    void parsesAllTarget() {
        ModerationHungerCommandParser.Request request = ModerationHungerCommandParser.parse(List.of("all", "20")).orElseThrow();

        assertTrue(request.target().all());
        assertEquals(20, request.amount());
    }

    @Test
    void rejectsMissingOrOutOfRangeAmounts() {
        assertTrue(ModerationHungerCommandParser.parse(List.of()).isEmpty());
        assertTrue(ModerationHungerCommandParser.parse(List.of("Steve")).isEmpty());
        assertTrue(ModerationHungerCommandParser.parse(List.of("Steve", "-1")).isEmpty());
        assertTrue(ModerationHungerCommandParser.parse(List.of("Steve", "21")).isEmpty());
        assertTrue(ModerationHungerCommandParser.parse(List.of("Steve", "many")).isEmpty());
    }
}
