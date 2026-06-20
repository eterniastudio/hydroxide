package net.axther.hydroxide.modules.moderation;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModerationEffectCommandParserTest {

    @Test
    void parsesEffectApplication() {
        ModerationEffectCommandParser.Request request = ModerationEffectCommandParser.parse(
                List.of("all", "speed", "60", "2", "-visual")
        ).orElseThrow();

        assertTrue(request.target().all());
        assertEquals(ModerationEffectCommandParser.Action.APPLY, request.action());
        assertEquals("speed", request.effect().orElseThrow());
        assertEquals(Duration.ofSeconds(60), request.duration());
        assertEquals(2, request.amplifier());
        assertTrue(request.particles());
    }

    @Test
    void parsesClearRequest() {
        ModerationEffectCommandParser.Request request = ModerationEffectCommandParser.parse(List.of("Steve", "clear")).orElseThrow();

        assertEquals("Steve", request.target().name());
        assertEquals(ModerationEffectCommandParser.Action.CLEAR, request.action());
        assertTrue(request.effect().isEmpty());
    }

    @Test
    void rejectsMissingOrInvalidArguments() {
        assertTrue(ModerationEffectCommandParser.parse(List.of()).isEmpty());
        assertTrue(ModerationEffectCommandParser.parse(List.of("Steve")).isEmpty());
        assertTrue(ModerationEffectCommandParser.parse(List.of("Steve", "speed", "0")).isEmpty());
        assertTrue(ModerationEffectCommandParser.parse(List.of("Steve", "speed", "60", "-1")).isEmpty());
    }
}
