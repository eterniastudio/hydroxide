package net.axther.hydroxide.modules.armorstand;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArmorStandLockPolicyTest {

    @Test
    void ownersAndBypassCanEditLockedStands() {
        UUID owner = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        ArmorStandLock lock = new ArmorStandLock(UUID.randomUUID(), owner, "world", 1, 2, 3);

        assertTrue(ArmorStandLockPolicy.canEdit(lock, owner, false));
        assertTrue(ArmorStandLockPolicy.canEdit(lock, other, true));
        assertFalse(ArmorStandLockPolicy.canEdit(lock, other, false));
    }
}
