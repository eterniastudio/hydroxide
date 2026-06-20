package net.axther.hydroxide.modules.moderation;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModerationFallDistanceCommandParserTest {

    @Test
    void parsesSinglePlayerDistance() {
        ModerationFallDistanceCommandParser.Request request = ModerationFallDistanceCommandParser.parse(List.of("Steve", "8.5")).orElseThrow();

        assertEquals("Steve", request.target().name());
        assertTrue(!request.target().all());
        assertEquals(8.5F, request.distance());
    }

    @Test
    void parsesAllTarget() {
        ModerationFallDistanceCommandParser.Request request = ModerationFallDistanceCommandParser.parse(List.of("all", "0")).orElseThrow();

        assertTrue(request.target().all());
        assertEquals(0.0F, request.distance());
    }

    @Test
    void rejectsMissingNegativeOrInvalidDistance() {
        assertTrue(ModerationFallDistanceCommandParser.parse(List.of()).isEmpty());
        assertTrue(ModerationFallDistanceCommandParser.parse(List.of("Steve")).isEmpty());
        assertTrue(ModerationFallDistanceCommandParser.parse(List.of("Steve", "-1")).isEmpty());
        assertTrue(ModerationFallDistanceCommandParser.parse(List.of("Steve", "many")).isEmpty());
    }
}
