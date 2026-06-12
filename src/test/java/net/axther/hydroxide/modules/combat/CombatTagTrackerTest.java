package net.axther.hydroxide.modules.combat;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CombatTagTrackerTest {

    @Test
    void tagsBothParticipantsUntilTheExpiryTime() {
        CombatTagTracker tracker = new CombatTagTracker(15_000L);
        UUID attacker = UUID.randomUUID();
        UUID victim = UUID.randomUUID();

        tracker.tag(attacker, victim, 1_000L);

        assertTrue(tracker.tagged(attacker, 10_000L));
        assertTrue(tracker.tagged(victim, 15_999L));
        assertFalse(tracker.tagged(attacker, 16_001L));
    }
}
