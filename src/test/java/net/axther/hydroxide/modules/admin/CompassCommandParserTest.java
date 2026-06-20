package net.axther.hydroxide.modules.admin;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompassCommandParserTest {

    @Test
    void parsesDirectionAndResetRequests() {
        CompassCommandParser.Request direction = CompassCommandParser.parse(List.of()).orElseThrow();
        CompassCommandParser.Request resetSelf = CompassCommandParser.parse(List.of("reset")).orElseThrow();
        CompassCommandParser.Request resetTarget = CompassCommandParser.parse(List.of("reset", "Alex", "-s")).orElseThrow();

        assertEquals(CompassCommandParser.Mode.DIRECTION, direction.mode());
        assertEquals(CompassCommandParser.Mode.RESET, resetSelf.mode());
        assertTrue(resetSelf.targetName().isEmpty());
        assertEquals("Alex", resetTarget.targetName().orElseThrow());
        assertTrue(resetTarget.silent());
    }

    @Test
    void parsesPlayerCompassTargetRequests() {
        CompassCommandParser.Request targetOnly = CompassCommandParser.parse(List.of("Alex")).orElseThrow();
        CompassCommandParser.Request targetAndSource = CompassCommandParser.parse(List.of("Alex", "Steve", "-s")).orElseThrow();

        assertEquals(CompassCommandParser.Mode.PLAYER, targetOnly.mode());
        assertEquals("Alex", targetOnly.targetName().orElseThrow());
        assertTrue(targetOnly.sourceName().isEmpty());
        assertEquals("Alex", targetAndSource.targetName().orElseThrow());
        assertEquals("Steve", targetAndSource.sourceName().orElseThrow());
        assertTrue(targetAndSource.silent());
    }

    @Test
    void parsesCoordinateCompassTargets() {
        CompassCommandParser.Request noWorld = CompassCommandParser.parse(List.of("Alex", "10", "-30", "-s")).orElseThrow();
        CompassCommandParser.Request withWorld = CompassCommandParser.parse(List.of("Alex", "10.5", "-30", "world_nether", "-s")).orElseThrow();

        assertEquals(CompassCommandParser.Mode.COORDINATES, noWorld.mode());
        assertEquals("Alex", noWorld.targetName().orElseThrow());
        assertEquals(10.0D, noWorld.x().orElseThrow());
        assertEquals(-30.0D, noWorld.z().orElseThrow());
        assertTrue(noWorld.worldName().isEmpty());
        assertEquals("world_nether", withWorld.worldName().orElseThrow());
        assertTrue(withWorld.silent());
    }

    @Test
    void rejectsUnknownFlagsAndMalformedCoordinates() {
        assertTrue(CompassCommandParser.parse(List.of("-x")).isEmpty());
        assertTrue(CompassCommandParser.parse(List.of("Alex", "-x")).isEmpty());
        assertTrue(CompassCommandParser.parse(List.of("Alex", "10")).isEmpty());
        assertTrue(CompassCommandParser.parse(List.of("Alex", "10", "nope")).isEmpty());
        assertTrue(CompassCommandParser.parse(List.of("Alex", "10", "20", "world", "-s", "extra")).isEmpty());
    }
}
