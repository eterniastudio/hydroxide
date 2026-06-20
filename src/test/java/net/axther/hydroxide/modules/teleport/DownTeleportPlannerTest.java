package net.axther.hydroxide.modules.teleport;

import org.junit.jupiter.api.Test;

import java.util.OptionalInt;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DownTeleportPlannerTest {

    @Test
    void findsFirstSafeFloorBelowPlayer() {
        DownTeleportPlanner.Column column = column(0, 100, Set.of(61, 40));

        OptionalInt destination = DownTeleportPlanner.findDestinationY(70, column, false);

        assertTrue(destination.isPresent());
        assertEquals(61, destination.orElseThrow());
    }

    @Test
    void maxFindsLowestSafeFloorBelowPlayer() {
        DownTeleportPlanner.Column column = column(0, 100, Set.of(61, 40));

        OptionalInt destination = DownTeleportPlanner.findDestinationY(70, column, true);

        assertTrue(destination.isPresent());
        assertEquals(40, destination.orElseThrow());
    }

    @Test
    void returnsEmptyWhenNoFloorExistsBelow() {
        DownTeleportPlanner.Column column = column(0, 100, Set.of(80));

        assertTrue(DownTeleportPlanner.findDestinationY(70, column, false).isEmpty());
    }

    private DownTeleportPlanner.Column column(int minY, int maxY, Set<Integer> safeFeetYs) {
        return new DownTeleportPlanner.Column() {
            @Override
            public int minY() {
                return minY;
            }

            @Override
            public int maxY() {
                return maxY;
            }

            @Override
            public boolean safeFeetAt(int feetY) {
                return safeFeetYs.contains(feetY);
            }
        };
    }
}
