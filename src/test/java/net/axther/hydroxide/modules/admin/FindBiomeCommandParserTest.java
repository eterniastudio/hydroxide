package net.axther.hydroxide.modules.admin;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FindBiomeCommandParserTest {

    @Test
    void parsesSearchStopAndStopAllRequests() {
        FindBiomeCommandParser.Request search = FindBiomeCommandParser.parse(List.of("cherry_grove")).orElseThrow();
        FindBiomeCommandParser.Request searchWithRadius = FindBiomeCommandParser.parse(List.of("minecraft:pale_garden", "-r:2500")).orElseThrow();
        FindBiomeCommandParser.Request stop = FindBiomeCommandParser.parse(List.of("stop")).orElseThrow();
        FindBiomeCommandParser.Request stopAll = FindBiomeCommandParser.parse(List.of("stopall")).orElseThrow();

        assertEquals(FindBiomeCommandParser.Action.SEARCH, search.action());
        assertEquals("cherry_grove", search.biomeName().orElseThrow());
        assertTrue(search.radius().isEmpty());
        assertEquals("minecraft:pale_garden", searchWithRadius.biomeName().orElseThrow());
        assertEquals(2500, searchWithRadius.radius().orElseThrow());
        assertEquals(FindBiomeCommandParser.Action.STOP, stop.action());
        assertEquals(FindBiomeCommandParser.Action.STOP_ALL, stopAll.action());
    }

    @Test
    void rejectsInvalidShapes() {
        assertTrue(FindBiomeCommandParser.parse(List.of()).isEmpty());
        assertTrue(FindBiomeCommandParser.parse(List.of("-r:1000")).isEmpty());
        assertTrue(FindBiomeCommandParser.parse(List.of("plains", "-r:0")).isEmpty());
        assertTrue(FindBiomeCommandParser.parse(List.of("plains", "-r:nope")).isEmpty());
        assertTrue(FindBiomeCommandParser.parse(List.of("plains", "extra")).isEmpty());
        assertTrue(FindBiomeCommandParser.parse(List.of("stop", "-r:100")).isEmpty());
    }
}
