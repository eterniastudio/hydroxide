package net.axther.hydroxide.modules.admin;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdminLightningCommandParserTest {

    @Test
    void parsesPlayerTargetWithSafeAndSilentFlags() {
        AdminLightningCommandParser.Request request = AdminLightningCommandParser.parse(List.of(
                "Alex", "-safe", "-s"
        )).orElseThrow();

        assertEquals(AdminLightningCommandParser.TargetType.PLAYER, request.targetType());
        assertEquals("Alex", request.targetName().orElseThrow());
        assertTrue(request.safe());
        assertTrue(request.silent());
    }

    @Test
    void parsesCoordinateTarget() {
        AdminLightningCommandParser.Request request = AdminLightningCommandParser.parse(List.of(
                "world;10.5;70;-22", "-safe"
        )).orElseThrow();

        assertEquals(AdminLightningCommandParser.TargetType.COORDINATES, request.targetType());
        assertEquals("world", request.coordinates().orElseThrow().worldName());
        assertEquals(10.5D, request.coordinates().orElseThrow().x(), 0.0001D);
        assertEquals(70.0D, request.coordinates().orElseThrow().y(), 0.0001D);
        assertEquals(-22.0D, request.coordinates().orElseThrow().z(), 0.0001D);
        assertTrue(request.safe());
    }

    @Test
    void rejectsMissingTargetDuplicateTargetAndUnknownFlags() {
        assertTrue(AdminLightningCommandParser.parse(List.of()).isEmpty());
        assertTrue(AdminLightningCommandParser.parse(List.of("Alex", "Steve")).isEmpty());
        assertTrue(AdminLightningCommandParser.parse(List.of("world;10;bad;20")).isEmpty());
        assertTrue(AdminLightningCommandParser.parse(List.of("Alex", "-boom")).isEmpty());
    }
}
