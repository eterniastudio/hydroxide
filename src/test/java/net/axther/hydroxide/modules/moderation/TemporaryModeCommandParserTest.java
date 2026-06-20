package net.axther.hydroxide.modules.moderation;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TemporaryModeCommandParserTest {

    @Test
    void parsesStatusFixedIndefiniteAndAdditiveDurations() {
        TemporaryModeCommandParser.Request status = TemporaryModeCommandParser.parse(List.of("Alex")).orElseThrow();
        TemporaryModeCommandParser.Request fixed = TemporaryModeCommandParser.parse(List.of("Alex", "30")).orElseThrow();
        TemporaryModeCommandParser.Request indefinite = TemporaryModeCommandParser.parse(List.of("Alex", "0")).orElseThrow();
        TemporaryModeCommandParser.Request additive = TemporaryModeCommandParser.parse(List.of("Alex", "+45")).orElseThrow();

        assertEquals("Alex", status.targetName());
        assertTrue(status.durationSeconds().isEmpty());
        assertEquals(30L, fixed.durationSeconds().orElseThrow());
        assertTrue(indefinite.indefinite());
        assertEquals(45L, additive.durationSeconds().orElseThrow());
        assertTrue(additive.additive());
    }

    @Test
    void parsesSilentFlag() {
        TemporaryModeCommandParser.Request request = TemporaryModeCommandParser.parse(List.of("Alex", "60", "-s")).orElseThrow();

        assertEquals("Alex", request.targetName());
        assertTrue(request.silent());
    }

    @Test
    void rejectsMissingTargetInvalidDurationOrExtraArguments() {
        assertTrue(TemporaryModeCommandParser.parse(List.of()).isEmpty());
        assertTrue(TemporaryModeCommandParser.parse(List.of("Alex", "soon")).isEmpty());
        assertTrue(TemporaryModeCommandParser.parse(List.of("Alex", "-1")).isEmpty());
        assertTrue(TemporaryModeCommandParser.parse(List.of("Alex", "1", "extra")).isEmpty());
    }
}
