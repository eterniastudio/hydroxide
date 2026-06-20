package net.axther.hydroxide.modules.admin;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdminLastOnlineCommandParserTest {

    @Test
    void defaultsToFirstPage() {
        AdminLastOnlineCommandParser.Request request = AdminLastOnlineCommandParser.parse(List.of()).orElseThrow();

        assertEquals(1, request.page());
    }

    @Test
    void parsesCmiPageFlagAndPlainPageNumber() {
        assertEquals(3, AdminLastOnlineCommandParser.parse(List.of("-p:3")).orElseThrow().page());
        assertEquals(2, AdminLastOnlineCommandParser.parse(List.of("2")).orElseThrow().page());
    }

    @Test
    void rejectsInvalidPagesAndExtraArguments() {
        assertTrue(AdminLastOnlineCommandParser.parse(List.of("-p:0")).isEmpty());
        assertTrue(AdminLastOnlineCommandParser.parse(List.of("-p:nope")).isEmpty());
        assertTrue(AdminLastOnlineCommandParser.parse(List.of("0")).isEmpty());
        assertTrue(AdminLastOnlineCommandParser.parse(List.of("2", "extra")).isEmpty());
    }
}
