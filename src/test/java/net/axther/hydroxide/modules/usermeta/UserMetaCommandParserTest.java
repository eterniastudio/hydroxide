package net.axther.hydroxide.modules.usermeta;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserMetaCommandParserTest {

    @Test
    void parsesAddWithMultiWordValueAndSilentFlag() {
        UserMetaCommandParser.Request request = UserMetaCommandParser
                .parse(List.of("Steve", "add", "display-title", "VIP", "Member", "-s"))
                .orElseThrow();

        assertEquals("Steve", request.playerName());
        assertEquals(UserMetaCommandParser.Action.ADD, request.action());
        assertEquals("display-title", request.key().orElseThrow());
        assertEquals("VIP Member", request.value().orElseThrow());
        assertTrue(request.silent());
    }

    @Test
    void parsesListAndClearWithoutKeys() {
        UserMetaCommandParser.Request list = UserMetaCommandParser
                .parse(List.of("Alex", "list"))
                .orElseThrow();
        UserMetaCommandParser.Request clear = UserMetaCommandParser
                .parse(List.of("Alex", "clear", "-s"))
                .orElseThrow();

        assertEquals(UserMetaCommandParser.Action.LIST, list.action());
        assertTrue(list.key().isEmpty());
        assertEquals(UserMetaCommandParser.Action.CLEAR, clear.action());
        assertTrue(clear.key().isEmpty());
        assertTrue(clear.silent());
    }

    @Test
    void parsesIncrementAndNormalizesKeysForPlaceholders() {
        UserMetaCommandParser.Request request = UserMetaCommandParser
                .parse(List.of("Alex", "increment", "Daily.Kills", "2.5"))
                .orElseThrow();

        assertEquals(UserMetaCommandParser.Action.INCREMENT, request.action());
        assertEquals("daily.kills", request.key().orElseThrow());
        assertEquals("2.5", request.value().orElseThrow());
    }

    @Test
    void rejectsMissingRequiredArgumentsAndUnknownActions() {
        assertTrue(UserMetaCommandParser.parse(List.of()).isEmpty());
        assertTrue(UserMetaCommandParser.parse(List.of("Alex", "unknown")).isEmpty());
        assertTrue(UserMetaCommandParser.parse(List.of("Alex", "add", "rank")).isEmpty());
        assertTrue(UserMetaCommandParser.parse(List.of("Alex", "remove")).isEmpty());
        assertTrue(UserMetaCommandParser.parse(List.of("Alex", "increment", "score")).isEmpty());
    }
}
