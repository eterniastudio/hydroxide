package net.axther.hydroxide.modules.teleport;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PatrolTargetSelectorTest {

    @Test
    void excludesThePatrollingPlayerAndUsesUnseenTargetsFirst() {
        PatrolTargetSelector selector = new PatrolTargetSelector(size -> 0);

        assertEquals("Alex", selector.next("Staff", List.of("Staff", "Alex", "Blake")).orElseThrow());
        assertEquals("Blake", selector.next("Staff", List.of("Staff", "Alex", "Blake")).orElseThrow());
    }

    @Test
    void startsANewCycleAfterAllTargetsHaveBeenVisited() {
        PatrolTargetSelector selector = new PatrolTargetSelector(size -> 0);
        selector.next("Staff", List.of("Staff", "Alex")).orElseThrow();

        assertEquals("Alex", selector.next("Staff", List.of("Staff", "Alex")).orElseThrow());
    }

    @Test
    void resetClearsVisitedTargetsForOnePatroller() {
        PatrolTargetSelector selector = new PatrolTargetSelector(size -> 0);
        selector.next("Staff", List.of("Staff", "Alex", "Blake")).orElseThrow();
        selector.reset("Staff");

        assertEquals("Alex", selector.next("Staff", List.of("Staff", "Alex", "Blake")).orElseThrow());
    }

    @Test
    void returnsEmptyWhenNoOtherPlayersAreOnline() {
        PatrolTargetSelector selector = new PatrolTargetSelector(size -> 0);

        Optional<String> result = selector.next("Staff", List.of("Staff"));

        assertTrue(result.isEmpty());
    }
}
