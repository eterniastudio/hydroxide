package net.axther.hydroxide.modules.utility;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

enum ItemRepairMode {
    HAND,
    ALL;

    static Optional<ItemRepairMode> from(List<String> args) {
        if (args.isEmpty()) {
            return Optional.of(HAND);
        }
        if (args.size() > 1) {
            return Optional.empty();
        }
        return switch (args.getFirst().toLowerCase(Locale.ROOT)) {
            case "hand", "held" -> Optional.of(HAND);
            case "all", "inventory" -> Optional.of(ALL);
            default -> Optional.empty();
        };
    }
}
