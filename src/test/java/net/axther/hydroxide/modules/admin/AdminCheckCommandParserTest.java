package net.axther.hydroxide.modules.admin;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdminCheckCommandParserTest {

    @Test
    void parsesOptionalKeyword() {
        assertTrue(AdminCheckCommandParser.parse(List.of()).orElseThrow().keyword().isEmpty());

        AdminCheckCommandParser.Request request = AdminCheckCommandParser
                .parse(List.of("teleport"))
                .orElseThrow();

        assertEquals("teleport", request.keyword().orElseThrow());
    }

    @Test
    void rejectsMultiWordKeyword() {
        assertTrue(AdminCheckCommandParser.parse(List.of("teleport", "admin")).isEmpty());
    }
}
