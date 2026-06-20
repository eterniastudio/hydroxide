package net.axther.hydroxide.modules.admin;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DonateCommandParserTest {

    @Test
    void parsesRequiredTargetWithOptionalAmountAndSilentFlag() {
        DonateCommandParser.Request targetOnly = DonateCommandParser.parse(List.of("Alex")).orElseThrow();
        DonateCommandParser.Request withAmount = DonateCommandParser.parse(List.of("Alex", "5")).orElseThrow();
        DonateCommandParser.Request silentTarget = DonateCommandParser.parse(List.of("Alex", "-s")).orElseThrow();
        DonateCommandParser.Request silentAmount = DonateCommandParser.parse(List.of("Alex", "5", "-s")).orElseThrow();

        assertEquals("Alex", targetOnly.targetName());
        assertTrue(targetOnly.amount().isEmpty());
        assertTrue(!targetOnly.silent());
        assertEquals(5, withAmount.amount().orElseThrow());
        assertEquals("Alex", silentTarget.targetName());
        assertTrue(silentTarget.amount().isEmpty());
        assertTrue(silentTarget.silent());
        assertEquals(5, silentAmount.amount().orElseThrow());
        assertTrue(silentAmount.silent());
    }

    @Test
    void rejectsMissingTargetInvalidAmountAndUnknownFlags() {
        assertTrue(DonateCommandParser.parse(List.of()).isEmpty());
        assertTrue(DonateCommandParser.parse(List.of("Alex", "0")).isEmpty());
        assertTrue(DonateCommandParser.parse(List.of("Alex", "-1")).isEmpty());
        assertTrue(DonateCommandParser.parse(List.of("Alex", "five")).isEmpty());
        assertTrue(DonateCommandParser.parse(List.of("Alex", "-x")).isEmpty());
        assertTrue(DonateCommandParser.parse(List.of("Alex", "5", "-x")).isEmpty());
        assertTrue(DonateCommandParser.parse(List.of("Alex", "5", "-s", "extra")).isEmpty());
    }
}
