package net.axther.hydroxide.modules.chat;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BroadcastCommandParserTest {

    @Test
    void parsesCleanWorldRangeAndCoordinateFilters() {
        BroadcastCommandParser.Request request = BroadcastCommandParser.parse(List.of(
                "!", "Server", "restart", "-w:world,world_nether", "-r:150", "-c:world;10.5;64;-30"
        )).orElseThrow();

        assertTrue(request.clean());
        assertEquals("Server restart", request.message());
        assertEquals(List.of("world", "world_nether"), request.worldNames());
        assertEquals(150.0D, request.radius().orElseThrow(), 0.0001D);
        assertEquals("world", request.center().orElseThrow().worldName());
        assertEquals(10.5D, request.center().orElseThrow().x(), 0.0001D);
        assertEquals(64.0D, request.center().orElseThrow().y(), 0.0001D);
        assertEquals(-30.0D, request.center().orElseThrow().z(), 0.0001D);
    }

    @Test
    void treatsLeadingBangOnMessageAsCleanBroadcast() {
        BroadcastCommandParser.Request request = BroadcastCommandParser.parse(List.of("!Maintenance", "soon")).orElseThrow();

        assertTrue(request.clean());
        assertEquals("Maintenance soon", request.message());
    }

    @Test
    void rejectsMissingMessageInvalidNumbersAndUnknownFlags() {
        assertTrue(BroadcastCommandParser.parse(List.of()).isEmpty());
        assertTrue(BroadcastCommandParser.parse(List.of("!", "-w:world")).isEmpty());
        assertTrue(BroadcastCommandParser.parse(List.of("Hello", "-r:0")).isEmpty());
        assertTrue(BroadcastCommandParser.parse(List.of("Hello", "-c:world;10;bad;30")).isEmpty());
        assertTrue(BroadcastCommandParser.parse(List.of("Hello", "-x:bad")).isEmpty());
    }
}
