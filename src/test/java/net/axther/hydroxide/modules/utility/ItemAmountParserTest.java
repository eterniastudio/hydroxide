package net.axther.hydroxide.modules.utility;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemAmountParserTest {

    @Test
    void defaultsToMaximumStackSize() {
        assertEquals(64, ItemAmountParser.targetAmount(List.of(), 64).orElseThrow());
    }

    @Test
    void acceptsPositiveAmountsAndCapsToMaximumStackSize() {
        assertEquals(32, ItemAmountParser.targetAmount(List.of("32"), 64).orElseThrow());
        assertEquals(16, ItemAmountParser.targetAmount(List.of("64"), 16).orElseThrow());
    }

    @Test
    void rejectsNonPositiveOrNonNumericAmounts() {
        assertTrue(ItemAmountParser.targetAmount(List.of("0"), 64).isEmpty());
        assertTrue(ItemAmountParser.targetAmount(List.of("-5"), 64).isEmpty());
        assertTrue(ItemAmountParser.targetAmount(List.of("many"), 64).isEmpty());
    }
}
