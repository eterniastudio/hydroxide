package net.axther.hydroxide.modules.admin;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdminSameIpCommandParserTest {

    @Test
    void parsesOptionalQuery() {
        assertTrue(AdminSameIpCommandParser.parse(List.of()).orElseThrow().query().isEmpty());

        AdminSameIpCommandParser.Request request = AdminSameIpCommandParser
                .parse(List.of("Steve"))
                .orElseThrow();

        assertEquals("Steve", request.query().orElseThrow());
    }

    @Test
    void rejectsExtraArguments() {
        assertTrue(AdminSameIpCommandParser.parse(List.of("Steve", "extra")).isEmpty());
    }
}
