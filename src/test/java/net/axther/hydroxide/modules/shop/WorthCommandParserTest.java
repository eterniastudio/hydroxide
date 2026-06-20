package net.axther.hydroxide.modules.shop;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorthCommandParserTest {

    @Test
    void worthDefaultsToHeldItem() {
        WorthCommandRequest request = WorthCommandParser.parseWorth(List.of()).orElseThrow();

        assertEquals(WorthCommandRequest.Source.HAND, request.source());
        assertTrue(request.material().isEmpty());
        assertTrue(request.amount().isEmpty());
    }

    @Test
    void worthParsesHeldItemAmount() {
        WorthCommandRequest request = WorthCommandParser.parseWorth(List.of("hand", "16")).orElseThrow();

        assertEquals(WorthCommandRequest.Source.HAND, request.source());
        assertEquals(16, request.amount().orElseThrow());
    }

    @Test
    void worthParsesMaterialAndAmount() {
        WorthCommandRequest request = WorthCommandParser.parseWorth(List.of("diamond", "3")).orElseThrow();

        assertEquals(WorthCommandRequest.Source.MATERIAL, request.source());
        assertEquals("diamond", request.material().orElseThrow());
        assertEquals(3, request.amount().orElseThrow());
    }

    @Test
    void setWorthParsesHeldItemPrice() {
        SetWorthCommandRequest request = WorthCommandParser.parseSetWorth(List.of("12.50")).orElseThrow();

        assertEquals(WorthCommandRequest.Source.HAND, request.source());
        assertTrue(request.material().isEmpty());
        assertEquals(12.50D, request.price());
    }

    @Test
    void setWorthParsesMaterialAndPrice() {
        SetWorthCommandRequest request = WorthCommandParser.parseSetWorth(List.of("diamond", "25")).orElseThrow();

        assertEquals(WorthCommandRequest.Source.MATERIAL, request.source());
        assertEquals("diamond", request.material().orElseThrow());
        assertEquals(25.0D, request.price());
    }

    @Test
    void rejectsUnsafeAmountsAndPrices() {
        assertTrue(WorthCommandParser.parseWorth(List.of("hand", "0")).isEmpty());
        assertTrue(WorthCommandParser.parseWorth(List.of("stone", "-1")).isEmpty());
        assertTrue(WorthCommandParser.parseWorth(List.of("stone", "1", "extra")).isEmpty());
        assertTrue(WorthCommandParser.parseSetWorth(List.of("stone", "NaN")).isEmpty());
        assertTrue(WorthCommandParser.parseSetWorth(List.of("stone", "Infinity")).isEmpty());
        assertTrue(WorthCommandParser.parseSetWorth(List.of("stone", "1.234")).isEmpty());
        assertTrue(WorthCommandParser.parseSetWorth(List.of("stone", "-1")).isEmpty());
        assertTrue(WorthCommandParser.parseSetWorth(List.of("stone", "1", "extra")).isEmpty());
    }
}
