package net.axther.hydroxide.modules.builder;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlockCyclingCommandParserTest {

    @Test
    void defaultsToForwardWhenNoDirectionIsProvided() {
        BlockCyclingCommandParser.Request request = BlockCyclingCommandParser.parse(List.of()).orElseThrow();

        assertEquals(BlockCyclingCommandParser.Direction.FORWARD, request.direction());
    }

    @Test
    void acceptsForwardAndBackwardAliases() {
        assertEquals(BlockCyclingCommandParser.Direction.FORWARD,
                BlockCyclingCommandParser.parse(List.of("next")).orElseThrow().direction());
        assertEquals(BlockCyclingCommandParser.Direction.BACKWARD,
                BlockCyclingCommandParser.parse(List.of("prev")).orElseThrow().direction());
    }

    @Test
    void rejectsUnknownOrExtraArguments() {
        assertTrue(BlockCyclingCommandParser.parse(List.of("sideways")).isEmpty());
        assertTrue(BlockCyclingCommandParser.parse(List.of("forward", "extra")).isEmpty());
    }
}
