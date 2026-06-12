package net.axther.hydroxide.modules.staff;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VanishVisibilityPolicyTest {

    @Test
    void visibleTargetsAreShownToEveryone() {
        assertFalse(VanishVisibilityPolicy.shouldHide(false, false, false, true));
        assertFalse(VanishVisibilityPolicy.shouldHide(false, false, true, true));
    }

    @Test
    void vanishedTargetsAreHiddenFromNormalViewers() {
        assertTrue(VanishVisibilityPolicy.shouldHide(true, false, false, true));
    }

    @Test
    void authorizedStaffCanSeeVanishedTargetsWhenConfigured() {
        assertFalse(VanishVisibilityPolicy.shouldHide(true, false, true, true));
        assertTrue(VanishVisibilityPolicy.shouldHide(true, false, true, false));
    }

    @Test
    void targetIsNeverHiddenFromSelf() {
        assertFalse(VanishVisibilityPolicy.shouldHide(true, true, false, false));
    }
}
