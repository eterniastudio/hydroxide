package net.axther.hydroxide.modules.utility;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FireworkCommandParserTest {

    @Test
    void parsesPowerWithDefaultAndExplicitAmount() {
        FireworkCommandParser.Request defaultPower = FireworkCommandParser.parse(List.of("power")).orElseThrow();
        FireworkCommandParser.Request explicitPower = FireworkCommandParser.parse(List.of("power", "3")).orElseThrow();

        assertEquals(FireworkCommandParser.Action.POWER, defaultPower.action());
        assertEquals(1, defaultPower.amount());
        assertEquals(FireworkCommandParser.Action.POWER, explicitPower.action());
        assertEquals(3, explicitPower.amount());
    }

    @Test
    void parsesClearWithoutAmount() {
        FireworkCommandParser.Request request = FireworkCommandParser.parse(List.of("clear")).orElseThrow();

        assertEquals(FireworkCommandParser.Action.CLEAR, request.action());
        assertEquals(0, request.amount());
    }

    @Test
    void parsesFireWithDefaultAndExplicitAmount() {
        FireworkCommandParser.Request defaultFire = FireworkCommandParser.parse(List.of("fire")).orElseThrow();
        FireworkCommandParser.Request explicitFire = FireworkCommandParser.parse(List.of("fire", "4")).orElseThrow();

        assertEquals(FireworkCommandParser.Action.FIRE, defaultFire.action());
        assertEquals(1, defaultFire.amount());
        assertEquals(FireworkCommandParser.Action.FIRE, explicitFire.action());
        assertEquals(4, explicitFire.amount());
    }

    @Test
    void rejectsInvalidForms() {
        assertTrue(FireworkCommandParser.parse(List.of()).isEmpty());
        assertTrue(FireworkCommandParser.parse(List.of("sparkle")).isEmpty());
        assertTrue(FireworkCommandParser.parse(List.of("power", "-1")).isEmpty());
        assertTrue(FireworkCommandParser.parse(List.of("power", "128")).isEmpty());
        assertTrue(FireworkCommandParser.parse(List.of("clear", "1")).isEmpty());
        assertTrue(FireworkCommandParser.parse(List.of("fire", "0")).isEmpty());
        assertTrue(FireworkCommandParser.parse(List.of("fire", "17")).isEmpty());
        assertTrue(FireworkCommandParser.parse(List.of("fire", "many")).isEmpty());
    }
}
