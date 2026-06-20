package net.axther.hydroxide.modules.teleport;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TeleportPositionParserTest {

    @Test
    void parsesAbsoluteCoordinates() {
        TeleportPositionParser.Coordinates coordinates = TeleportPositionParser
                .coordinates("12.5", "64", "-30", 1.0D, 2.0D, 3.0D)
                .orElseThrow();

        assertEquals(12.5D, coordinates.x());
        assertEquals(64.0D, coordinates.y());
        assertEquals(-30.0D, coordinates.z());
    }

    @Test
    void parsesRelativeCoordinates() {
        TeleportPositionParser.Coordinates coordinates = TeleportPositionParser
                .coordinates("~2", "~-1.5", "~", 10.0D, 70.0D, -4.0D)
                .orElseThrow();

        assertEquals(12.0D, coordinates.x());
        assertEquals(68.5D, coordinates.y());
        assertEquals(-4.0D, coordinates.z());
    }

    @Test
    void rejectsInvalidCoordinatesAndUnsafeYValues() {
        assertTrue(TeleportPositionParser.coordinates("x", "64", "0", 0, 64, 0).isEmpty());
        assertTrue(TeleportPositionParser.coordinates("0", "-65", "0", 0, 64, 0).isEmpty());
        assertTrue(TeleportPositionParser.coordinates("0", "4097", "0", 0, 64, 0).isEmpty());
    }

    @Test
    void parsesPositionRequestWithTargetWorldPitchAndYaw() {
        TeleportPositionParser.Request request = TeleportPositionParser
                .request(List.of("-p:Alex", "~1", "70", "~-2", "world_nether", "15", "90"))
                .orElseThrow();

        assertEquals("Alex", request.targetName().orElseThrow());
        assertEquals("~1", request.x());
        assertEquals("70", request.y());
        assertEquals("~-2", request.z());
        assertEquals("world_nether", request.worldName().orElseThrow());
        assertEquals(15.0F, request.pitch().orElseThrow());
        assertEquals(90.0F, request.yaw().orElseThrow());
    }

    @Test
    void parsesPositionRequestWithoutTarget() {
        TeleportPositionParser.Request request = TeleportPositionParser
                .request(List.of("1", "2", "3"))
                .orElseThrow();

        assertTrue(request.targetName().isEmpty());
        assertTrue(request.worldName().isEmpty());
        assertTrue(request.pitch().isEmpty());
        assertTrue(request.yaw().isEmpty());
    }

    @Test
    void rejectsMalformedPositionRequests() {
        assertTrue(TeleportPositionParser.request(List.of("-p:", "1", "2", "3")).isEmpty());
        assertTrue(TeleportPositionParser.request(List.of("1", "2")).isEmpty());
        assertTrue(TeleportPositionParser.request(List.of("1", "2", "3", "world", "pitch")).isEmpty());
        assertTrue(TeleportPositionParser.request(List.of("1", "2", "3", "world", "1", "2", "extra")).isEmpty());
        assertFalse(TeleportPositionParser.request(List.of("-p:Alex", "1", "2", "3")).isEmpty());
    }
}
