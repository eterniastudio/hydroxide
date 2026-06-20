package net.axther.hydroxide.modules.admin;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdminLaunchCommandParserTest {

    @Test
    void defaultsToSenderWhenNoArgumentsAreProvided() {
        AdminLaunchCommandParser.Request request = AdminLaunchCommandParser.parse(List.of()).orElseThrow();

        assertTrue(request.targetName().isEmpty());
        assertTrue(request.power().isEmpty());
        assertTrue(request.angle().isEmpty());
        assertTrue(request.directionDegrees().isEmpty());
        assertTrue(request.locationTarget().isEmpty());
        assertFalse(request.noDamage());
        assertFalse(request.silent());
    }

    @Test
    void parsesTargetAndMotionOptions() {
        AdminLaunchCommandParser.Request request = AdminLaunchCommandParser.parse(
                List.of("Alex", "p:3.2", "a:25", "d:east", "-nodamage", "-s")
        ).orElseThrow();

        assertEquals("Alex", request.targetName().orElseThrow());
        assertEquals(3.2D, request.power().orElseThrow(), 0.0001D);
        assertEquals(25.0D, request.angle().orElseThrow(), 0.0001D);
        assertEquals(-90.0D, request.directionDegrees().orElseThrow(), 0.0001D);
        assertTrue(request.noDamage());
        assertTrue(request.silent());
    }

    @Test
    void parsesLocationTarget() {
        AdminLaunchCommandParser.Request request = AdminLaunchCommandParser.parse(
                List.of("loc:150:120:123", "p:2.5")
        ).orElseThrow();

        AdminLaunchCommandParser.LocationTarget location = request.locationTarget().orElseThrow();
        assertEquals(150.0D, location.x(), 0.0001D);
        assertEquals(120.0D, location.y(), 0.0001D);
        assertEquals(123.0D, location.z(), 0.0001D);
        assertEquals(2.5D, request.power().orElseThrow(), 0.0001D);
        assertTrue(request.targetName().isEmpty());
    }

    @Test
    void rejectsInvalidOptionsAndDuplicateSingularValues() {
        assertTrue(AdminLaunchCommandParser.parse(List.of("p:nope")).isEmpty());
        assertTrue(AdminLaunchCommandParser.parse(List.of("a:91")).isEmpty());
        assertTrue(AdminLaunchCommandParser.parse(List.of("d:up")).isEmpty());
        assertTrue(AdminLaunchCommandParser.parse(List.of("loc:1:2")).isEmpty());
        assertTrue(AdminLaunchCommandParser.parse(List.of("Alex", "Steve")).isEmpty());
        assertTrue(AdminLaunchCommandParser.parse(List.of("p:1", "p:2")).isEmpty());
    }
}
