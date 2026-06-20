package net.axther.hydroxide.modules.economy;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChequeAmountParserTest {

    @Test
    void acceptsPositiveAmountsWithAtMostTwoDecimals() {
        assertEquals(10.0, ChequeAmountParser.parse("10").orElseThrow());
        assertEquals(10.5, ChequeAmountParser.parse("10.50").orElseThrow());
        assertEquals(0.01, ChequeAmountParser.parse("0.01").orElseThrow());
    }

    @Test
    void rejectsUnsafeOrInvalidAmounts() {
        assertTrue(ChequeAmountParser.parse("0").isEmpty());
        assertTrue(ChequeAmountParser.parse("-1").isEmpty());
        assertTrue(ChequeAmountParser.parse("1.234").isEmpty());
        assertTrue(ChequeAmountParser.parse("NaN").isEmpty());
        assertTrue(ChequeAmountParser.parse("Infinity").isEmpty());
        assertTrue(ChequeAmountParser.parse("not-money").isEmpty());
    }
}
