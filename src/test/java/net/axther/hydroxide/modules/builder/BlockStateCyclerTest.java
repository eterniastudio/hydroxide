package net.axther.hydroxide.modules.builder;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlockStateCyclerTest {

    @Test
    void cyclesForwardWithWrapAround() {
        List<String> values = List.of("north", "east", "south", "west");

        assertEquals("south", BlockStateCycler.cycle(values, "east", BlockCyclingCommandParser.Direction.FORWARD).orElseThrow());
        assertEquals("north", BlockStateCycler.cycle(values, "west", BlockCyclingCommandParser.Direction.FORWARD).orElseThrow());
    }

    @Test
    void cyclesBackwardWithWrapAround() {
        List<String> values = List.of("north", "east", "south", "west");

        assertEquals("north", BlockStateCycler.cycle(values, "east", BlockCyclingCommandParser.Direction.BACKWARD).orElseThrow());
        assertEquals("west", BlockStateCycler.cycle(values, "north", BlockCyclingCommandParser.Direction.BACKWARD).orElseThrow());
    }

    @Test
    void rejectsUnknownCurrentValueOrSingleValueLists() {
        assertTrue(BlockStateCycler.cycle(List.of("north", "east"), "south", BlockCyclingCommandParser.Direction.FORWARD).isEmpty());
        assertTrue(BlockStateCycler.cycle(List.of("north"), "north", BlockCyclingCommandParser.Direction.FORWARD).isEmpty());
    }
}
