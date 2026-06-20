package net.axther.hydroxide.modules.admin;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdminCheckAccountCommandParserTest {

    @Test
    void parsesSinglePlayerNameIpOrHashQuery() {
        AdminCheckAccountCommandParser.Request request = AdminCheckAccountCommandParser
                .parse(List.of("Steve"))
                .orElseThrow();

        assertEquals("Steve", request.query());
    }

    @Test
    void rejectsMissingOrExtraArguments() {
        assertTrue(AdminCheckAccountCommandParser.parse(List.of()).isEmpty());
        assertTrue(AdminCheckAccountCommandParser.parse(List.of("Steve", "extra")).isEmpty());
    }
}
