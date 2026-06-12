package net.axther.hydroxide.modules.stats;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LeaderboardTest {

    @Test
    void sortsEntriesDescendingAndLimitsResults() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        UUID third = UUID.randomUUID();

        Leaderboard leaderboard = new Leaderboard(Map.of(first, 2L, second, 8L, third, 5L));

        assertEquals(second, leaderboard.top(2).get(0).playerId());
        assertEquals(third, leaderboard.top(2).get(1).playerId());
        assertEquals(2, leaderboard.top(2).size());
    }
}
