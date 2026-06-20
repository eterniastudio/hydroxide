package net.axther.hydroxide.modules.jail;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JailReleaseCommandParserTest {

    @Test
    void parsesTarget() {
        JailReleaseCommandParser.Request request = JailReleaseCommandParser.parse(List.of("Steve")).orElseThrow();

        assertEquals("Steve", request.targetName());
        assertFalse(request.silent());
    }

    @Test
    void parsesSilentFlagBeforeOrAfterTarget() {
        JailReleaseCommandParser.Request leading = JailReleaseCommandParser.parse(List.of("-s", "Steve")).orElseThrow();
        JailReleaseCommandParser.Request trailing = JailReleaseCommandParser.parse(List.of("Steve", "-s")).orElseThrow();

        assertEquals("Steve", leading.targetName());
        assertTrue(leading.silent());
        assertEquals("Steve", trailing.targetName());
        assertTrue(trailing.silent());
    }

    @Test
    void rejectsMissingOrExtraTargets() {
        assertTrue(JailReleaseCommandParser.parse(List.of()).isEmpty());
        assertTrue(JailReleaseCommandParser.parse(List.of("-s")).isEmpty());
        assertTrue(JailReleaseCommandParser.parse(List.of("Steve", "Alex")).isEmpty());
    }
}
