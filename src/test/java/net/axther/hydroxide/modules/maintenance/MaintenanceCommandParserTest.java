package net.axther.hydroxide.modules.maintenance;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MaintenanceCommandParserTest {

    @Test
    void parsesCmiStyleEnableWithMessage() {
        MaintenanceCommandParser.Request request = MaintenanceCommandParser
                .parse(List.of("true", "Restart", "in", "5m"))
                .orElseThrow();

        assertEquals(MaintenanceCommandParser.Action.ENABLE, request.action());
        assertEquals("Restart in 5m", request.message().orElseThrow());
    }

    @Test
    void parsesDisableAliases() {
        MaintenanceCommandParser.Request request = MaintenanceCommandParser
                .parse(List.of("off"))
                .orElseThrow();

        assertEquals(MaintenanceCommandParser.Action.DISABLE, request.action());
        assertTrue(request.message().isEmpty());
    }

    @Test
    void defaultsEmptyCommandToStatus() {
        MaintenanceCommandParser.Request request = MaintenanceCommandParser
                .parse(List.of())
                .orElseThrow();

        assertEquals(MaintenanceCommandParser.Action.STATUS, request.action());
    }

    @Test
    void rejectsUnknownMode() {
        assertTrue(MaintenanceCommandParser.parse(List.of("maybe")).isEmpty());
    }
}
