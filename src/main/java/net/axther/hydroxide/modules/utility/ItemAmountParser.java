package net.axther.hydroxide.modules.utility;

import java.util.List;
import java.util.OptionalInt;

final class ItemAmountParser {

    private ItemAmountParser() {
    }

    static OptionalInt targetAmount(List<String> args, int maximumStackSize) {
        int safeMaximum = Math.max(1, maximumStackSize);
        if (args.isEmpty()) {
            return OptionalInt.of(safeMaximum);
        }
        try {
            int requested = Integer.parseInt(args.get(0));
            if (requested <= 0) {
                return OptionalInt.empty();
            }
            return OptionalInt.of(Math.min(requested, safeMaximum));
        } catch (NumberFormatException exception) {
            return OptionalInt.empty();
        }
    }
}
