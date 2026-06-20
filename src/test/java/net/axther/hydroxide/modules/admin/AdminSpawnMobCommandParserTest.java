package net.axther.hydroxide.modules.admin;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdminSpawnMobCommandParserTest {

    @Test
    void parsesEntityOnlyWithDefaults() {
        AdminSpawnMobCommandParser.Request request = AdminSpawnMobCommandParser.parse(List.of("zombie")).orElseThrow();

        assertEquals("zombie", request.entityName());
        assertTrue(request.amount().isEmpty());
        assertTrue(request.targetName().isEmpty());
        assertFalse(request.silent());
    }

    @Test
    void parsesAmountTargetAndSilentFlag() {
        AdminSpawnMobCommandParser.Request request = AdminSpawnMobCommandParser.parse(List.of("minecraft:skeleton", "4", "Alex", "-s")).orElseThrow();

        assertEquals("minecraft:skeleton", request.entityName());
        assertEquals(4, request.amount().orElseThrow());
        assertEquals("Alex", request.targetName().orElseThrow());
        assertTrue(request.silent());
    }

    @Test
    void parsesTargetWithoutAmount() {
        AdminSpawnMobCommandParser.Request request = AdminSpawnMobCommandParser.parse(List.of("cow", "Steve")).orElseThrow();

        assertEquals("cow", request.entityName());
        assertTrue(request.amount().isEmpty());
        assertEquals("Steve", request.targetName().orElseThrow());
    }

    @Test
    void rejectsMalformedRequests() {
        assertTrue(AdminSpawnMobCommandParser.parse(List.of()).isEmpty());
        assertTrue(AdminSpawnMobCommandParser.parse(List.of("-s")).isEmpty());
        assertTrue(AdminSpawnMobCommandParser.parse(List.of("zombie", "0")).isEmpty());
        assertTrue(AdminSpawnMobCommandParser.parse(List.of("zombie", "-1")).isEmpty());
        assertTrue(AdminSpawnMobCommandParser.parse(List.of("zombie", "3", "Alex", "extra")).isEmpty());
        assertTrue(AdminSpawnMobCommandParser.parse(List.of("zombie", "-x")).isEmpty());
    }
}
