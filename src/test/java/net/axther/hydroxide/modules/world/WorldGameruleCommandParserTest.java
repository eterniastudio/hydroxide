package net.axther.hydroxide.modules.world;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorldGameruleCommandParserTest {

    @Test
    void parsesPlayerLocalWorldRequests() {
        WorldGameruleCommandParser.Request request = WorldGameruleCommandParser
                .parse(List.of("doDaylightCycle", "false"), true)
                .orElseThrow();

        assertTrue(request.worldName().isEmpty());
        assertEquals("doDaylightCycle", request.setting());
        assertEquals("false", request.value());
    }

    @Test
    void parsesExplicitWorldRequests() {
        WorldGameruleCommandParser.Request request = WorldGameruleCommandParser
                .parse(List.of("world_nether", "pvp", "false"), false)
                .orElseThrow();

        assertEquals("world_nether", request.worldName().orElseThrow());
        assertEquals("pvp", request.setting());
        assertEquals("false", request.value());
    }

    @Test
    void rejectsIncompleteRequests() {
        assertTrue(WorldGameruleCommandParser.parse(List.of(), true).isEmpty());
        assertTrue(WorldGameruleCommandParser.parse(List.of("pvp"), true).isEmpty());
        assertTrue(WorldGameruleCommandParser.parse(List.of("pvp", "true"), false).isEmpty());
        assertTrue(WorldGameruleCommandParser.parse(List.of("world", "pvp", "true", "extra"), true).isEmpty());
    }
}
