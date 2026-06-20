package net.axther.hydroxide.modules.jail;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JailCommandParserTest {

    @Test
    void parsesLegacyCellDurationReasonOrder() {
        JailCommandParser.Request request = JailCommandParser
                .parse(List.of("Steve", "spawn", "60", "griefing"), List.of("spawn"), Duration.ofMinutes(5))
                .orElseThrow();

        assertEquals("Steve", request.targetName());
        assertEquals("spawn", request.jailName().orElseThrow());
        assertEquals(Duration.ofSeconds(60), request.duration());
        assertEquals("griefing", request.reason().orElseThrow());
        assertFalse(request.silent());
    }

    @Test
    void parsesCmiStyleDurationJailSilentAndReasonFlag() {
        JailCommandParser.Request request = JailCommandParser
                .parse(List.of("Steve", "10m", "spawn", "-s", "r:Rule", "break"), List.of("spawn"), Duration.ofMinutes(5))
                .orElseThrow();

        assertEquals("Steve", request.targetName());
        assertEquals("spawn", request.jailName().orElseThrow());
        assertEquals(Duration.ofMinutes(10), request.duration());
        assertEquals("Rule break", request.reason().orElseThrow());
        assertTrue(request.silent());
    }

    @Test
    void defaultsDurationWhenOnlyJailNameIsProvided() {
        JailCommandParser.Request request = JailCommandParser
                .parse(List.of("Steve", "spawn"), List.of("spawn"), Duration.ofMinutes(5))
                .orElseThrow();

        assertEquals("spawn", request.jailName().orElseThrow());
        assertEquals(Duration.ofMinutes(5), request.duration());
        assertTrue(request.reason().isEmpty());
    }

    @Test
    void acceptsOptionalCmiCellIdWithoutTreatingItAsReason() {
        JailCommandParser.Request request = JailCommandParser
                .parse(List.of("Steve", "10m", "spawn", "2", "r:minor", "theft"), List.of("spawn"), Duration.ofMinutes(5))
                .orElseThrow();

        assertEquals("spawn", request.jailName().orElseThrow());
        assertEquals("2", request.cellId().orElseThrow());
        assertEquals("minor theft", request.reason().orElseThrow());
    }

    @Test
    void rejectsMissingTargetOrInvalidDuration() {
        assertTrue(JailCommandParser.parse(List.of(), List.of("spawn"), Duration.ofMinutes(5)).isEmpty());
        assertTrue(JailCommandParser.parse(List.of("Steve", "spawn", "0s"), List.of("spawn"), Duration.ofMinutes(5)).isEmpty());
        assertTrue(JailCommandParser.parse(List.of("Steve", "spawn", "forever"), List.of("spawn"), Duration.ofMinutes(5)).isEmpty());
    }
}
