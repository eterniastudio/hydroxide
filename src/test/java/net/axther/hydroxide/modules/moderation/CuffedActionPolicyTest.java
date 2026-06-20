package net.axther.hydroxide.modules.moderation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CuffedActionPolicyTest {

    @Test
    void cancelsSuspendedActionsOnlyForCuffedPlayers() {
        for (CuffedActionPolicy.Action action : CuffedActionPolicy.Action.values()) {
            assertTrue(CuffedActionPolicy.shouldCancel(true, action));
            assertFalse(CuffedActionPolicy.shouldCancel(false, action));
        }
    }

    @Test
    void allowsRecoveryCommandsWhileCuffed() {
        assertFalse(CuffedActionPolicy.shouldCancelCommand(true, "/cuff Alex false"));
        assertFalse(CuffedActionPolicy.shouldCancelCommand(true, "/hydroxide reload"));
        assertTrue(CuffedActionPolicy.shouldCancelCommand(true, "/spawn"));
        assertFalse(CuffedActionPolicy.shouldCancelCommand(false, "/spawn"));
    }
}
