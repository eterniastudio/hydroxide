package net.axther.hydroxide.modules.chat;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MuteChatCommandParserTest {

    @Test
    void emptyInputEnablesForDefaultDuration() {
        MuteChatCommandParser.Request request = MuteChatCommandParser.parse(List.of(), Duration.ofHours(1)).orElseThrow();

        assertEquals(MuteChatCommandParser.Action.ENABLE, request.action());
        assertEquals(Duration.ofHours(1), request.duration().orElseThrow());
        assertFalse(request.silent());
        assertEquals("", request.reason());
    }

    @Test
    void parsesCompoundDurationSilentFlagAndReason() {
        MuteChatCommandParser.Request request = MuteChatCommandParser.parse(
                List.of("1h30m", "-s", "maintenance", "window"),
                Duration.ofHours(1)
        ).orElseThrow();

        assertEquals(MuteChatCommandParser.Action.ENABLE, request.action());
        assertEquals(Duration.ofMinutes(90), request.duration().orElseThrow());
        assertTrue(request.silent());
        assertEquals("maintenance window", request.reason());
    }

    @Test
    void parsesStatusAndDisableActions() {
        assertEquals(MuteChatCommandParser.Action.STATUS,
                MuteChatCommandParser.parse(List.of("status"), Duration.ofHours(1)).orElseThrow().action());
        assertEquals(MuteChatCommandParser.Action.DISABLE,
                MuteChatCommandParser.parse(List.of("off"), Duration.ofHours(1)).orElseThrow().action());
        assertEquals(MuteChatCommandParser.Action.DISABLE,
                MuteChatCommandParser.parse(List.of("clear"), Duration.ofHours(1)).orElseThrow().action());
    }

    @Test
    void rejectsInvalidDurationAndExtraControlArguments() {
        assertTrue(MuteChatCommandParser.parse(List.of("later"), Duration.ofHours(1)).isEmpty());
        assertTrue(MuteChatCommandParser.parse(List.of("status", "now"), Duration.ofHours(1)).isEmpty());
    }
}
