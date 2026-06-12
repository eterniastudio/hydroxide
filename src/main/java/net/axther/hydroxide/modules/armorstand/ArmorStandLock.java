package net.axther.hydroxide.modules.armorstand;

import java.util.UUID;

public record ArmorStandLock(UUID standId, UUID ownerId, String worldName, int x, int y, int z) {
}
