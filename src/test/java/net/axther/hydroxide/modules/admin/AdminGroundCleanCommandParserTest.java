package net.axther.hydroxide.modules.admin;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdminGroundCleanCommandParserTest {

    @Test
    void parsesCmiGroundCleanFlags() {
        AdminGroundCleanCommandParser.Request request = AdminGroundCleanCommandParser
                .parseGroundClean(List.of("+cm", "+cb", "+ci", "+b", "+tnt", "+sh", "-r:80", "-w:world_nether"))
                .orElseThrow();

        assertTrue(request.categories().contains(AdminGroundCleanCommandParser.Category.DROPS));
        assertTrue(request.categories().contains(AdminGroundCleanCommandParser.Category.MINECARTS));
        assertTrue(request.categories().contains(AdminGroundCleanCommandParser.Category.BOATS));
        assertTrue(request.categories().contains(AdminGroundCleanCommandParser.Category.TNT));
        assertTrue(request.includeGearDrops());
        assertTrue(request.includeShulkerDrops());
        assertTrue(request.broadcast());
        assertEquals(80, request.radius().orElseThrow());
        assertEquals("world_nether", request.worldName().orElseThrow());
    }

    @Test
    void parsesEssentialsRemoveSelectorWithRadius() {
        AdminGroundCleanCommandParser.Request request = AdminGroundCleanCommandParser
                .parseRemove(List.of("drops", "25"))
                .orElseThrow();

        assertEquals(List.of(AdminGroundCleanCommandParser.Category.DROPS), request.categories().stream().toList());
        assertEquals(25, request.radius().orElseThrow());
        assertTrue(request.worldName().isEmpty());
    }

    @Test
    void parsesEssentialsRemoveAllWithWorld() {
        AdminGroundCleanCommandParser.Request request = AdminGroundCleanCommandParser
                .parseRemove(List.of("all", "world"))
                .orElseThrow();

        assertTrue(request.categories().contains(AdminGroundCleanCommandParser.Category.ALL));
        assertEquals("world", request.worldName().orElseThrow());
        assertTrue(request.radius().isEmpty());
    }

    @Test
    void protectsSensitiveDropsByDefault() {
        AdminGroundCleanCommandParser.Request request = AdminGroundCleanCommandParser
                .parseGroundClean(List.of())
                .orElseThrow();

        assertFalse(request.includeGearDrops());
        assertFalse(request.includeShulkerDrops());
        assertEquals(List.of(AdminGroundCleanCommandParser.Category.DROPS), request.categories().stream().toList());
    }

    @Test
    void rejectsInvalidArguments() {
        assertTrue(AdminGroundCleanCommandParser.parseGroundClean(List.of("-r:not-a-number")).isEmpty());
        assertTrue(AdminGroundCleanCommandParser.parseGroundClean(List.of("+unknown")).isEmpty());
        assertTrue(AdminGroundCleanCommandParser.parseRemove(List.of()).isEmpty());
        assertTrue(AdminGroundCleanCommandParser.parseRemove(List.of("drops", "25", "extra")).isEmpty());
        assertTrue(AdminGroundCleanCommandParser.parseRemove(List.of("unknown")).isEmpty());
    }
}
