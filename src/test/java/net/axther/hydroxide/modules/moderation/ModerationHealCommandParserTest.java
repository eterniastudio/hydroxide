package net.axther.hydroxide.modules.moderation;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModerationHealCommandParserTest {

    @Test
    void parsesSelfFullHealWhenNoArgumentsAreGiven() {
        ModerationHealCommandParser.Request request = ModerationHealCommandParser.parse(List.of()).orElseThrow();

        assertFalse(request.target().all());
        assertTrue(request.target().name().isEmpty());
        assertEquals(ModerationHealCommandParser.HealAmount.full(), request.amount());
    }

    @Test
    void parsesTargetFullHeal() {
        ModerationHealCommandParser.Request request = ModerationHealCommandParser.parse(List.of("Steve")).orElseThrow();

        assertEquals("Steve", request.target().name().orElseThrow());
        assertFalse(request.target().all());
        assertEquals(ModerationHealCommandParser.HealAmount.full(), request.amount());
    }

    @Test
    void parsesAllTargetWithAbsoluteAmount() {
        ModerationHealCommandParser.Request request = ModerationHealCommandParser.parse(List.of("all", "10")).orElseThrow();

        assertTrue(request.target().all());
        assertEquals(ModerationHealCommandParser.HealAmount.absolute(10.0D), request.amount());
    }

    @Test
    void parsesSelfPercentAmount() {
        ModerationHealCommandParser.Request request = ModerationHealCommandParser.parse(List.of("25%")).orElseThrow();

        assertTrue(request.target().name().isEmpty());
        assertEquals(ModerationHealCommandParser.HealAmount.percent(25.0D), request.amount());
    }

    @Test
    void parsesTargetPercentAmount() {
        ModerationHealCommandParser.Request request = ModerationHealCommandParser.parse(List.of("Alex", "50%")).orElseThrow();

        assertEquals("Alex", request.target().name().orElseThrow());
        assertEquals(ModerationHealCommandParser.HealAmount.percent(50.0D), request.amount());
    }

    @Test
    void rejectsInvalidAmountsAndExtraArguments() {
        assertTrue(ModerationHealCommandParser.parse(List.of("Alex", "NaN")).isEmpty());
        assertTrue(ModerationHealCommandParser.parse(List.of("Alex", "Infinity")).isEmpty());
        assertTrue(ModerationHealCommandParser.parse(List.of("Alex", "-1")).isEmpty());
        assertTrue(ModerationHealCommandParser.parse(List.of("Alex", "0")).isEmpty());
        assertTrue(ModerationHealCommandParser.parse(List.of("Alex", "101%")).isEmpty());
        assertTrue(ModerationHealCommandParser.parse(List.of("Alex", "10", "extra")).isEmpty());
    }
}
