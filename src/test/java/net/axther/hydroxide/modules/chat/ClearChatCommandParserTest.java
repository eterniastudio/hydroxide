package net.axther.hydroxide.modules.chat;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClearChatCommandParserTest {

    @Test
    void defaultsToGlobalClear() {
        ClearChatCommandParser.Request request = ClearChatCommandParser.parse(List.of()).orElseThrow();

        assertEquals(ClearChatCommandParser.Mode.GLOBAL, request.mode());
        assertTrue(!request.silent());
    }

    @Test
    void parsesSelfAndSilentInAnyOrder() {
        ClearChatCommandParser.Request selfSilent = ClearChatCommandParser.parse(List.of("self", "-s")).orElseThrow();
        ClearChatCommandParser.Request silentSelf = ClearChatCommandParser.parse(List.of("-s", "self")).orElseThrow();

        assertEquals(ClearChatCommandParser.Mode.SELF, selfSilent.mode());
        assertTrue(selfSilent.silent());
        assertEquals(ClearChatCommandParser.Mode.SELF, silentSelf.mode());
        assertTrue(silentSelf.silent());
    }

    @Test
    void rejectsUnknownModesAndDuplicateValues() {
        assertTrue(ClearChatCommandParser.parse(List.of("all")).isEmpty());
        assertTrue(ClearChatCommandParser.parse(List.of("self", "extra")).isEmpty());
        assertTrue(ClearChatCommandParser.parse(List.of("-x")).isEmpty());
    }
}
