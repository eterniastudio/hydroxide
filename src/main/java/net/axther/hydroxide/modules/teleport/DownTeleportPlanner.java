package net.axther.hydroxide.modules.teleport;

import java.util.OptionalInt;

final class DownTeleportPlanner {

    private DownTeleportPlanner() {
    }

    static OptionalInt findDestinationY(int startFeetY, Column column, boolean max) {
        OptionalInt candidate = OptionalInt.empty();
        for (int y = Math.min(startFeetY - 1, column.maxY() - 1); y >= column.minY() + 1; y--) {
            if (!column.safeFeetAt(y)) {
                continue;
            }
            if (!max) {
                return OptionalInt.of(y);
            }
            candidate = OptionalInt.of(y);
        }
        return candidate;
    }

    interface Column {
        int minY();

        int maxY();

        boolean safeFeetAt(int feetY);
    }
}
