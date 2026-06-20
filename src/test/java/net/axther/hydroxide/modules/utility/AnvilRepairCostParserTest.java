package net.axther.hydroxide.modules.utility;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnvilRepairCostParserTest {

    @Test
    void parsesNonNegativeRepairCost() {
        assertEquals(0, AnvilRepairCostParser.parse(List.of("0")).orElseThrow());
        assertEquals(27, AnvilRepairCostParser.parse(List.of("27")).orElseThrow());
    }

    @Test
    void rejectsMissingNegativeOrNonNumericCost() {
        assertTrue(AnvilRepairCostParser.parse(List.of()).isEmpty());
        assertTrue(AnvilRepairCostParser.parse(List.of("-1")).isEmpty());
        assertTrue(AnvilRepairCostParser.parse(List.of("abc")).isEmpty());
        assertTrue(AnvilRepairCostParser.parse(List.of("1", "extra")).isEmpty());
    }
}
