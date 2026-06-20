package net.axther.hydroxide.modules.teleport;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TpAllWorldCommandParserTest {

    @Test
    void parsesSourceWorldOnly() {
        TpAllWorldCommandParser.Request request = TpAllWorldCommandParser.parse(List.of("world")).orElseThrow();

        assertEquals("world", request.sourceWorld());
        assertTrue(request.destination().isEmpty());
        assertFalse(request.includeAll());
    }

    @Test
    void parsesDestinationSpecAndIncludeAllFlag() {
        TpAllWorldCommandParser.Request request = TpAllWorldCommandParser
                .parse(List.of("world_nether", "world;10.5;72;-4;90;15", "-a"))
                .orElseThrow();

        TpAllWorldCommandParser.Destination destination = request.destination().orElseThrow();
        assertEquals("world_nether", request.sourceWorld());
        assertEquals("world", destination.worldName());
        assertEquals(10.5D, destination.x());
        assertEquals(72.0D, destination.y());
        assertEquals(-4.0D, destination.z());
        assertEquals(90.0F, destination.yaw());
        assertEquals(15.0F, destination.pitch());
        assertTrue(request.includeAll());
    }

    @Test
    void rejectsMissingSourceInvalidDestinationOrExtraArguments() {
        assertTrue(TpAllWorldCommandParser.parse(List.of()).isEmpty());
        assertTrue(TpAllWorldCommandParser.parse(List.of("world", "bad;10;20")).isEmpty());
        assertTrue(TpAllWorldCommandParser.parse(List.of("world", "world;10;20;30", "extra")).isEmpty());
    }
}
