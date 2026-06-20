package net.axther.hydroxide.modules.shop;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GenerateWorthCommandParserTest {

    @Test
    void parsesDefaults() {
        GenerateWorthCommandParser.Request request = GenerateWorthCommandParser.parse(List.of()).orElseThrow();

        assertEquals(1.0D, request.basePrice());
        assertFalse(request.overwrite());
    }

    @Test
    void parsesBasePriceAndOverwriteFlag() {
        GenerateWorthCommandParser.Request request = GenerateWorthCommandParser.parse(List.of("2.50", "-overwrite")).orElseThrow();

        assertEquals(2.50D, request.basePrice());
        assertTrue(request.overwrite());
    }

    @Test
    void rejectsInvalidBasePricesAndExtraTokens() {
        assertTrue(GenerateWorthCommandParser.parse(List.of("0")).isEmpty());
        assertTrue(GenerateWorthCommandParser.parse(List.of("1.234")).isEmpty());
        assertTrue(GenerateWorthCommandParser.parse(List.of("abc")).isEmpty());
        assertTrue(GenerateWorthCommandParser.parse(List.of("1.00", "2.00")).isEmpty());
    }
}
