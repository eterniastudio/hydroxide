package net.axther.hydroxide.modules.moderation;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModerationKickCommandParserTest {

    @Test
    void parsesSinglePlayerWithNoReason() {
        ModerationKickCommandParser.Request request = ModerationKickCommandParser.parse(List.of("Steve")).orElseThrow();

        assertFalse(request.target().all());
        assertEquals("Steve", request.target().name());
        assertTrue(request.reason().isEmpty());
    }

    @Test
    void parsesSinglePlayerWithReason() {
        ModerationKickCommandParser.Request request = ModerationKickCommandParser.parse(List.of("Steve", "Rule", "break")).orElseThrow();

        assertEquals("Steve", request.target().name());
        assertEquals("Rule break", request.reason().orElseThrow());
    }

    @Test
    void parsesSilentFlagWithoutAddingItToReason() {
        ModerationKickCommandParser.Request targetOnly = ModerationKickCommandParser.parse(List.of("Steve", "-s")).orElseThrow();
        ModerationKickCommandParser.Request reasonAfterFlag = ModerationKickCommandParser.parse(List.of("Steve", "-s", "Rule", "break")).orElseThrow();
        ModerationKickCommandParser.Request flagAfterReason = ModerationKickCommandParser.parse(List.of("Steve", "Rule", "break", "-s")).orElseThrow();

        assertTrue(targetOnly.silent());
        assertTrue(targetOnly.reason().isEmpty());
        assertTrue(reasonAfterFlag.silent());
        assertEquals("Rule break", reasonAfterFlag.reason().orElseThrow());
        assertTrue(flagAfterReason.silent());
        assertEquals("Rule break", flagAfterReason.reason().orElseThrow());
    }

    @Test
    void parsesAllTargetAliases() {
        assertTrue(ModerationKickCommandParser.parse(List.of("all")).orElseThrow().target().all());
        assertTrue(ModerationKickCommandParser.parse(List.of("*")).orElseThrow().target().all());
        assertTrue(ModerationKickCommandParser.parse(List.of("@a")).orElseThrow().target().all());
    }

    @Test
    void parsesAllTargetWithReason() {
        ModerationKickCommandParser.Request request = ModerationKickCommandParser.parse(List.of("all", "Maintenance", "restart")).orElseThrow();

        assertTrue(request.target().all());
        assertEquals("Maintenance restart", request.reason().orElseThrow());
    }

    @Test
    void rejectsMissingTarget() {
        assertTrue(ModerationKickCommandParser.parse(List.of()).isEmpty());
    }
}
