package net.axther.hydroxide.modules.moderation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NoTargetPolicyTest {

    @Test
    void cancelsOnlyWhenTargetIsProtectedPlayer() {
        assertTrue(NoTargetPolicy.shouldCancel(true, true));
        assertFalse(NoTargetPolicy.shouldCancel(true, false));
        assertFalse(NoTargetPolicy.shouldCancel(false, true));
        assertFalse(NoTargetPolicy.shouldCancel(false, false));
    }
}
