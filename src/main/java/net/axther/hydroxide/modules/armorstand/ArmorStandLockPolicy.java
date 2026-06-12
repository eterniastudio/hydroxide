package net.axther.hydroxide.modules.armorstand;

import java.util.UUID;

public final class ArmorStandLockPolicy {

    private ArmorStandLockPolicy() {
    }

    public static boolean canEdit(ArmorStandLock lock, UUID playerId, boolean bypass) {
        return lock == null || bypass || lock.ownerId().equals(playerId);
    }
}
