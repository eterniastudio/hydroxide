package net.axther.hydroxide.modules.environment;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnvironmentCommandParserTest {

    @Test
    void parsesCommonTimeAliasesAndTicks() {
        assertEquals(1000L, EnvironmentCommandParser.time("day").orElseThrow());
        assertEquals(6000L, EnvironmentCommandParser.time("noon").orElseThrow());
        assertEquals(13000L, EnvironmentCommandParser.time("night").orElseThrow());
        assertEquals(18000L, EnvironmentCommandParser.time("midnight").orElseThrow());
        assertEquals(23999L, EnvironmentCommandParser.time("23999").orElseThrow());
        assertTrue(EnvironmentCommandParser.time("24000").isEmpty());
    }

    @Test
    void parsesWeatherAliases() {
        assertEquals(EnvironmentWeatherMode.CLEAR, EnvironmentCommandParser.weather("sun").orElseThrow());
        assertEquals(EnvironmentWeatherMode.CLEAR, EnvironmentCommandParser.weather("clear").orElseThrow());
        assertEquals(EnvironmentWeatherMode.RAIN, EnvironmentCommandParser.weather("storm").orElseThrow());
        assertEquals(EnvironmentWeatherMode.RAIN, EnvironmentCommandParser.weather("rain").orElseThrow());
        assertEquals(EnvironmentWeatherMode.THUNDER, EnvironmentCommandParser.weather("thunder").orElseThrow());
        assertTrue(EnvironmentCommandParser.weather("wind").isEmpty());
    }
}
