package net.axther.hydroxide.modules.admin;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnderChestCommandParserTest {

    @Test
    void defaultsToSenderAsSourceAndViewer() {
        EnderChestCommandParser.Request request = EnderChestCommandParser.parse(List.of()).orElseThrow();

        assertTrue(request.sourceName().isEmpty());
        assertTrue(request.viewerName().isEmpty());
        assertTrue(!request.silent());
    }

    @Test
    void parsesSourceOnlyAndTwoPlayerForms() {
        EnderChestCommandParser.Request sourceOnly = EnderChestCommandParser.parse(List.of("Alex")).orElseThrow();
        EnderChestCommandParser.Request twoPlayers = EnderChestCommandParser.parse(List.of("Alex", "Blake")).orElseThrow();

        assertEquals("Alex", sourceOnly.sourceName().orElseThrow());
        assertTrue(sourceOnly.viewerName().isEmpty());
        assertEquals("Alex", twoPlayers.sourceName().orElseThrow());
        assertEquals("Blake", twoPlayers.viewerName().orElseThrow());
    }

    @Test
    void parsesSilentFlagForSelfSourceAndTwoPlayerForms() {
        EnderChestCommandParser.Request silentSelf = EnderChestCommandParser.parse(List.of("-s")).orElseThrow();
        EnderChestCommandParser.Request silentSource = EnderChestCommandParser.parse(List.of("Alex", "-s")).orElseThrow();
        EnderChestCommandParser.Request silentTwoPlayers = EnderChestCommandParser.parse(List.of("Alex", "Blake", "-s")).orElseThrow();

        assertTrue(silentSelf.silent());
        assertEquals("Alex", silentSource.sourceName().orElseThrow());
        assertTrue(silentSource.viewerName().isEmpty());
        assertEquals("Alex", silentTwoPlayers.sourceName().orElseThrow());
        assertEquals("Blake", silentTwoPlayers.viewerName().orElseThrow());
        assertTrue(silentTwoPlayers.silent());
    }

    @Test
    void rejectsUnknownFlagsAndExtraArguments() {
        assertTrue(EnderChestCommandParser.parse(List.of("-x")).isEmpty());
        assertTrue(EnderChestCommandParser.parse(List.of("Alex", "-x")).isEmpty());
        assertTrue(EnderChestCommandParser.parse(List.of("Alex", "Blake", "-s", "extra")).isEmpty());
    }
}
