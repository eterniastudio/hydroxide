package net.axther.hydroxide.modules.afk;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ActivityTrackerTest {

    @Test
    void marksPlayerAfkAfterIdleWindowAndClearsOnActivity() {
        ActivityTracker tracker = new ActivityTracker(300_000L);
        UUID playerId = UUID.randomUUID();

        tracker.recordActivity(playerId, 0L);

        assertFalse(tracker.afk(playerId, 299_999L));
        assertTrue(tracker.afk(playerId, 300_001L));
        tracker.recordActivity(playerId, 300_100L);
        assertFalse(tracker.afk(playerId, 300_101L));
    }
}
