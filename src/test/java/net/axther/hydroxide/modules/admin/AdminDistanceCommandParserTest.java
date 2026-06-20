package net.axther.hydroxide.modules.admin;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdminDistanceCommandParserTest {

    @Test
    void parsesTargetOnlyRequest() {
        AdminDistanceCommandParser.Request request = AdminDistanceCommandParser.parse(List.of("Alex")).orElseThrow();

        assertEquals("Alex", request.firstPlayer());
        assertTrue(request.secondPlayer().isEmpty());
    }

    @Test
    void parsesTwoPlayerRequest() {
        AdminDistanceCommandParser.Request request = AdminDistanceCommandParser.parse(List.of("Alex", "Steve")).orElseThrow();

        assertEquals("Alex", request.firstPlayer());
        assertEquals("Steve", request.secondPlayer().orElseThrow());
    }

    @Test
    void rejectsMissingOrExtraArguments() {
        assertTrue(AdminDistanceCommandParser.parse(List.of()).isEmpty());
        assertTrue(AdminDistanceCommandParser.parse(List.of("")).isEmpty());
        assertTrue(AdminDistanceCommandParser.parse(List.of("Alex", "Steve", "Herobrine")).isEmpty());
    }
}
