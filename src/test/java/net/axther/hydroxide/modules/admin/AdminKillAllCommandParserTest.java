package net.axther.hydroxide.modules.admin;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdminKillAllCommandParserTest {

    @Test
    void defaultsToMonsterCleanup() {
        AdminKillAllCommandParser.Request request = AdminKillAllCommandParser.parse(List.of()).orElseThrow();

        assertTrue(request.categories().contains(AdminKillAllCommandParser.Category.MONSTERS));
        assertFalse(request.includeNamed());
        assertFalse(request.lightning());
        assertTrue(request.radius().isEmpty());
        assertTrue(request.worldName().isEmpty());
        assertTrue(request.entityType().isEmpty());
    }

    @Test
    void parsesCategoryAndSafetyFlags() {
        AdminKillAllCommandParser.Request request = AdminKillAllCommandParser.parse(List.of(
                "-animals", "-pets", "-named", "-lightning"
        )).orElseThrow();

        assertTrue(request.categories().contains(AdminKillAllCommandParser.Category.ANIMALS));
        assertTrue(request.categories().contains(AdminKillAllCommandParser.Category.PETS));
        assertTrue(request.includeNamed());
        assertTrue(request.lightning());
    }

    @Test
    void parsesWorldRadiusAndTypeFilters() {
        AdminKillAllCommandParser.Request request = AdminKillAllCommandParser.parse(List.of(
                "-w:world_nether", "-r:128.5", "-m:zombie"
        )).orElseThrow();

        assertEquals("world_nether", request.worldName().orElseThrow());
        assertEquals(128.5D, request.radius().orElseThrow());
        assertEquals("zombie", request.entityType().orElseThrow());
    }

    @Test
    void parsesListModeWithoutDefaultCategory() {
        AdminKillAllCommandParser.Request request = AdminKillAllCommandParser.parse(List.of("-list")).orElseThrow();

        assertTrue(request.listMode());
        assertTrue(request.categories().isEmpty());
    }

    @Test
    void rejectsInvalidArguments() {
        assertTrue(AdminKillAllCommandParser.parse(List.of("-r:0")).isEmpty());
        assertTrue(AdminKillAllCommandParser.parse(List.of("-r:NaN")).isEmpty());
        assertTrue(AdminKillAllCommandParser.parse(List.of("-w:")).isEmpty());
        assertTrue(AdminKillAllCommandParser.parse(List.of("-m:")).isEmpty());
        assertTrue(AdminKillAllCommandParser.parse(List.of("animals")).isEmpty());
    }
}
