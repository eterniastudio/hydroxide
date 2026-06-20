package net.axther.hydroxide.modules.admin;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdminAlertCommandParserTest {

    @Test
    void parsesAddWithOptionalReasonAndSilentFlag() {
        AdminAlertCommandParser.Request request = AdminAlertCommandParser
                .parse(List.of("add", "Steve", "possible", "alt", "-s"))
                .orElseThrow();

        assertEquals(AdminAlertCommandParser.Action.ADD, request.action());
        assertEquals("Steve", request.playerName().orElseThrow());
        assertEquals("possible alt", request.reason().orElseThrow());
        assertTrue(request.silent());
    }

    @Test
    void parsesListWithoutTarget() {
        AdminAlertCommandParser.Request request = AdminAlertCommandParser
                .parse(List.of("list"))
                .orElseThrow();

        assertEquals(AdminAlertCommandParser.Action.LIST, request.action());
        assertTrue(request.playerName().isEmpty());
        assertTrue(request.reason().isEmpty());
    }

    @Test
    void parsesRemoveWithTarget() {
        AdminAlertCommandParser.Request request = AdminAlertCommandParser
                .parse(List.of("remove", "Steve"))
                .orElseThrow();

        assertEquals(AdminAlertCommandParser.Action.REMOVE, request.action());
        assertEquals("Steve", request.playerName().orElseThrow());
    }

    @Test
    void rejectsMissingTargetsAndUnknownActions() {
        assertTrue(AdminAlertCommandParser.parse(List.of()).isEmpty());
        assertTrue(AdminAlertCommandParser.parse(List.of("add")).isEmpty());
        assertTrue(AdminAlertCommandParser.parse(List.of("remove")).isEmpty());
        assertTrue(AdminAlertCommandParser.parse(List.of("list", "extra")).isEmpty());
        assertTrue(AdminAlertCommandParser.parse(List.of("bogus", "Steve")).isEmpty());
    }
}
