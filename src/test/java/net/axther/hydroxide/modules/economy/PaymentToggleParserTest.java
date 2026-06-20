package net.axther.hydroxide.modules.economy;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaymentToggleParserTest {

    @Test
    void emptyInputTogglesCurrentState() {
        assertFalse(PaymentToggleParser.parse(List.of(), true).orElseThrow());
        assertTrue(PaymentToggleParser.parse(List.of(), false).orElseThrow());
    }

    @Test
    void parsesExplicitStates() {
        assertTrue(PaymentToggleParser.parse(List.of("on"), false).orElseThrow());
        assertTrue(PaymentToggleParser.parse(List.of("enable"), false).orElseThrow());
        assertFalse(PaymentToggleParser.parse(List.of("off"), true).orElseThrow());
        assertFalse(PaymentToggleParser.parse(List.of("disable"), true).orElseThrow());
    }

    @Test
    void rejectsUnknownOrExtraArguments() {
        assertTrue(PaymentToggleParser.parse(List.of("maybe"), true).isEmpty());
        assertTrue(PaymentToggleParser.parse(List.of("on", "extra"), true).isEmpty());
    }
}
