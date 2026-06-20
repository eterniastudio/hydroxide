package net.axther.hydroxide.modules.jail;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JailToggleCommandParserTest {

    @Test
    void parsesReleaseStyleTargetAndSilentFlag() {
        JailToggleCommandParser.Request request = JailToggleCommandParser
                .parse(List.of("Steve", "-s"), List.of("spawn"), Duration.ofMinutes(5))
                .orElseThrow();

        assertEquals("Steve", request.targetName());
        assertTrue(request.jailName().isEmpty());
        assertEquals(Duration.ofMinutes(5), request.duration());
        assertTrue(request.silent());
    }

    @Test
    void parsesCmiStyleJailArgumentsForToggle() {
        JailToggleCommandParser.Request request = JailToggleCommandParser
                .parse(List.of("Steve", "10m", "spawn", "2", "r:Rule", "break"), List.of("spawn"), Duration.ofMinutes(5))
                .orElseThrow();

        assertEquals("Steve", request.targetName());
        assertEquals("spawn", request.jailName().orElseThrow());
        assertEquals("2", request.cellId().orElseThrow());
        assertEquals(Duration.ofMinutes(10), request.duration());
        assertEquals("Rule break", request.reason().orElseThrow());
    }

    @Test
    void rejectsMissingTargetOrInvalidDuration() {
        assertTrue(JailToggleCommandParser.parse(List.of(), List.of("spawn"), Duration.ofMinutes(5)).isEmpty());
        assertTrue(JailToggleCommandParser.parse(List.of("Steve", "0s"), List.of("spawn"), Duration.ofMinutes(5)).isEmpty());
        assertTrue(JailToggleCommandParser.parse(List.of("Steve", "spawn", "forever"), List.of("spawn"), Duration.ofMinutes(5)).isEmpty());
    }
}
