package net.axther.hydroxide.modules.shop;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorthListCommandParserTest {

    @Test
    void parsesDefaults() {
        WorthListCommandParser.Request request = WorthListCommandParser.parse(List.of()).orElseThrow();

        assertTrue(request.target().isEmpty());
        assertFalse(request.missing());
        assertEquals(1, request.page());
    }

    @Test
    void parsesTargetMissingAndPageInAnyOrder() {
        WorthListCommandParser.Request request = WorthListCommandParser.parse(List.of("Steve", "-missing", "-p:3")).orElseThrow();

        assertEquals("Steve", request.target().orElseThrow());
        assertTrue(request.missing());
        assertEquals(3, request.page());
    }

    @Test
    void parsesNumericPageAndMissingAlias() {
        WorthListCommandParser.Request request = WorthListCommandParser.parse(List.of("missing", "2")).orElseThrow();

        assertTrue(request.target().isEmpty());
        assertTrue(request.missing());
        assertEquals(2, request.page());
    }

    @Test
    void rejectsAmbiguousOrInvalidArguments() {
        assertTrue(WorthListCommandParser.parse(List.of("-p:0")).isEmpty());
        assertTrue(WorthListCommandParser.parse(List.of("1", "2")).isEmpty());
        assertTrue(WorthListCommandParser.parse(List.of("Steve", "Alex")).isEmpty());
        assertTrue(WorthListCommandParser.parse(List.of("-missing", "missing")).isEmpty());
    }
}
