package net.axther.hydroxide.modules.staff;

import java.util.UUID;

public record VanishChange(
        UUID playerId,
        boolean vanishedBefore,
        boolean vanishedNow,
        boolean reconcile,
        boolean restoreVisualState,
        VanishReason reason
) {
}
