package net.axther.hydroxide.modules.kit;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KitCommandParserTest {

    @Test
    void parsesMenuRequestWhenNoArgumentsAreProvided() {
        KitCommandParser.Request request = KitCommandParser.parse(List.of()).orElseThrow();

        assertEquals(KitCommandParser.Mode.MENU, request.mode());
        assertTrue(request.kit().isEmpty());
        assertTrue(request.target().isEmpty());
    }

    @Test
    void parsesSelfClaimRequest() {
        KitCommandParser.Request request = KitCommandParser.parse(List.of("starter")).orElseThrow();

        assertEquals(KitCommandParser.Mode.CLAIM, request.mode());
        assertEquals("starter", request.kit().orElseThrow());
        assertTrue(request.target().isEmpty());
        assertFalse(request.silent());
    }

    @Test
    void parsesTargetedSilentClaimRequest() {
        KitCommandParser.Request request = KitCommandParser.parse(List.of("starter", "Alex", "-s")).orElseThrow();

        assertEquals(KitCommandParser.Mode.CLAIM, request.mode());
        assertEquals("starter", request.kit().orElseThrow());
        assertEquals("Alex", request.target().orElseThrow());
        assertTrue(request.silent());
    }

    @Test
    void parsesPreviewAndOpenFlags() {
        assertEquals(KitCommandParser.Mode.PREVIEW, KitCommandParser.parse(List.of("starter", "-preview")).orElseThrow().mode());
        assertEquals(KitCommandParser.Mode.OPEN, KitCommandParser.parse(List.of("starter", "Alex", "-open", "-c")).orElseThrow().mode());
        assertTrue(KitCommandParser.parse(List.of("starter", "Alex", "-open", "-c")).orElseThrow().ignoreCooldown());
    }

    @Test
    void rejectsInvalidArguments() {
        assertTrue(KitCommandParser.parse(List.of("-preview")).isEmpty());
        assertTrue(KitCommandParser.parse(List.of("starter", "Alex", "Steve")).isEmpty());
        assertTrue(KitCommandParser.parse(List.of("starter", "-unknown")).isEmpty());
        assertTrue(KitCommandParser.parse(List.of("starter", "-preview", "-open")).isEmpty());
    }
}
