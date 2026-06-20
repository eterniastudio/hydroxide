package net.axther.hydroxide.modules.world;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class UnloadChunksCommandParserTest {

    @Test
    void parsesDefaultAndForcedModes() {
        UnloadChunksCommandParser.Request regular = UnloadChunksCommandParser.parse(List.of()).orElseThrow();
        UnloadChunksCommandParser.Request forced = UnloadChunksCommandParser.parse(List.of("-f")).orElseThrow();

        assertTrue(!regular.forced());
        assertTrue(forced.forced());
    }

    @Test
    void rejectsUnknownFlagsAndExtraArguments() {
        assertTrue(UnloadChunksCommandParser.parse(List.of("-x")).isEmpty());
        assertTrue(UnloadChunksCommandParser.parse(List.of("world")).isEmpty());
        assertTrue(UnloadChunksCommandParser.parse(List.of("-f", "extra")).isEmpty());
    }
}
