package net.axther.hydroxide.modules.admin;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdminSpawnerCommandParserTest {

    @Test
    void parsesEntityWithOptionalTargetAndSilentFlag() {
        AdminSpawnerCommandParser.Request self = AdminSpawnerCommandParser.parse(List.of("zombie")).orElseThrow();
        AdminSpawnerCommandParser.Request target = AdminSpawnerCommandParser.parse(List.of("minecraft:skeleton", "Alex")).orElseThrow();
        AdminSpawnerCommandParser.Request silent = AdminSpawnerCommandParser.parse(List.of("cow", "Alex", "-s")).orElseThrow();

        assertEquals("zombie", self.entityName());
        assertTrue(self.targetName().isEmpty());
        assertTrue(!self.silent());
        assertEquals("minecraft:skeleton", target.entityName());
        assertEquals("Alex", target.targetName().orElseThrow());
        assertEquals("cow", silent.entityName());
        assertTrue(silent.silent());
    }

    @Test
    void rejectsMissingEntityUnknownFlagsAndExtraArguments() {
        assertTrue(AdminSpawnerCommandParser.parse(List.of()).isEmpty());
        assertTrue(AdminSpawnerCommandParser.parse(List.of("-s")).isEmpty());
        assertTrue(AdminSpawnerCommandParser.parse(List.of("zombie", "-x")).isEmpty());
        assertTrue(AdminSpawnerCommandParser.parse(List.of("zombie", "Alex", "-x")).isEmpty());
        assertTrue(AdminSpawnerCommandParser.parse(List.of("zombie", "Alex", "-s", "extra")).isEmpty());
    }
}
