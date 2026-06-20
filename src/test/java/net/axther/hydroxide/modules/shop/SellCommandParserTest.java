package net.axther.hydroxide.modules.shop;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SellCommandParserTest {

    @Test
    void defaultsToHandSale() {
        SellCommandRequest request = SellCommandParser.parse("sell", List.of()).orElseThrow();

        assertEquals(SellCommandRequest.Mode.HAND, request.mode());
        assertTrue(request.amount().isEmpty());
        assertTrue(request.material().isEmpty());
    }

    @Test
    void parsesHandAmounts() {
        SellCommandRequest request = SellCommandParser.parse("sell", List.of("hand", "16")).orElseThrow();

        assertEquals(SellCommandRequest.Mode.HAND, request.mode());
        assertEquals(16, request.amount().orElseThrow());
    }

    @Test
    void parsesMaterialSales() {
        SellCommandRequest request = SellCommandParser.parse("sell", List.of("stone", "32")).orElseThrow();

        assertEquals(SellCommandRequest.Mode.MATERIAL, request.mode());
        assertEquals("stone", request.material().orElseThrow());
        assertEquals(32, request.amount().orElseThrow());
    }

    @Test
    void parsesAllSalesAndSellAllAlias() {
        assertEquals(SellCommandRequest.Mode.ALL, SellCommandParser.parse("sell", List.of("all")).orElseThrow().mode());
        assertEquals(SellCommandRequest.Mode.ALL, SellCommandParser.parse("sell", List.of("inventory")).orElseThrow().mode());
        assertEquals(SellCommandRequest.Mode.ALL, SellCommandParser.parse("sellall", List.of()).orElseThrow().mode());
    }

    @Test
    void rejectsUnsafeAmountsAndExtraArguments() {
        assertTrue(SellCommandParser.parse("sell", List.of("hand", "0")).isEmpty());
        assertTrue(SellCommandParser.parse("sell", List.of("hand", "-1")).isEmpty());
        assertTrue(SellCommandParser.parse("sell", List.of("stone", "1.5")).isEmpty());
        assertTrue(SellCommandParser.parse("sell", List.of("all", "extra")).isEmpty());
        assertTrue(SellCommandParser.parse("sell", List.of("stone", "1", "extra")).isEmpty());
    }
}
