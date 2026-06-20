package net.axther.hydroxide.modules.chat;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PrivateChatCommandParserTest {

    @Test
    void noArgumentsRequestsStatus() {
        PrivateChatCommandParser.Request request = PrivateChatCommandParser.parse(List.of()).orElseThrow();

        assertEquals(PrivateChatCommandParser.Action.STATUS, request.action());
        assertTrue(request.targetName().isEmpty());
    }

    @Test
    void offClearsFocusedTarget() {
        PrivateChatCommandParser.Request request = PrivateChatCommandParser.parse(List.of("off")).orElseThrow();

        assertEquals(PrivateChatCommandParser.Action.CLEAR, request.action());
    }

    @Test
    void playerNameFocusesTarget() {
        PrivateChatCommandParser.Request request = PrivateChatCommandParser.parse(List.of("Alex")).orElseThrow();

        assertEquals(PrivateChatCommandParser.Action.FOCUS, request.action());
        assertEquals("Alex", request.targetName().orElseThrow());
    }

    @Test
    void rejectsBlankOrExtraArguments() {
        assertTrue(PrivateChatCommandParser.parse(List.of(" ")).isEmpty());
        assertTrue(PrivateChatCommandParser.parse(List.of("Alex", "extra")).isEmpty());
    }
}
