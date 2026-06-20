package net.axther.hydroxide.commands.framework;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommandArgumentParserTest {

    private final CommandArgumentParser parser = new CommandArgumentParser();

    @Test
    void parsesFiniteMoneyWithAtMostTwoDecimals() {
        assertEquals(12.5D, parser.money("12.50").orElseThrow());

        assertTrue(parser.money("NaN").isEmpty());
        assertTrue(parser.money("Infinity").isEmpty());
        assertTrue(parser.money("1.234").isEmpty());
        assertTrue(parser.money("-1").isEmpty());
    }

    @Test
    void parsesDurationsWithExplicitUnits() {
        assertEquals(Duration.ofSeconds(10), parser.duration("10s").orElseThrow());
        assertEquals(Duration.ofMinutes(5), parser.duration("5m").orElseThrow());
        assertEquals(Duration.ofHours(2), parser.duration("2h").orElseThrow());
        assertEquals(Duration.ofDays(1), parser.duration("1d").orElseThrow());

        assertTrue(parser.duration("15").isEmpty());
        assertTrue(parser.duration("soon").isEmpty());
    }

    @Test
    void parsesUuidAndEnumValues() {
        UUID uuid = UUID.randomUUID();

        assertEquals(uuid, parser.uuid(uuid.toString()).orElseThrow());
        assertEquals(Sample.SECOND, parser.enumValue(Sample.class, "second").orElseThrow());
        assertTrue(parser.enumValue(Sample.class, "missing").isEmpty());
    }

    private enum Sample {
        FIRST,
        SECOND
    }
}
