package net.axther.hydroxide.modules.admin;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdminSudoAllCommandParserTest {

    @Test
    void parsesCommandExecutionForAllPlayers() {
        AdminSudoAllCommandParser.Request request = AdminSudoAllCommandParser.parse(List.of("command", "warp", "event")).orElseThrow();

        assertEquals(AdminSudoAllCommandParser.Mode.COMMAND, request.mode());
        assertEquals("warp event", request.value());
    }

    @Test
    void parsesChatExecutionForAllPlayers() {
        AdminSudoAllCommandParser.Request request = AdminSudoAllCommandParser.parse(List.of("chat", "hello", "team")).orElseThrow();

        assertEquals(AdminSudoAllCommandParser.Mode.CHAT, request.mode());
        assertEquals("hello team", request.value());
    }

    @Test
    void acceptsCommonCommandAliases() {
        assertEquals(AdminSudoAllCommandParser.Mode.COMMAND,
                AdminSudoAllCommandParser.parse(List.of("cmd", "/spawn")).orElseThrow().mode());
    }

    @Test
    void rejectsMissingModeOrValue() {
        assertTrue(AdminSudoAllCommandParser.parse(List.of()).isEmpty());
        assertTrue(AdminSudoAllCommandParser.parse(List.of("chat")).isEmpty());
        assertTrue(AdminSudoAllCommandParser.parse(List.of("run", "spawn")).isEmpty());
    }
}
