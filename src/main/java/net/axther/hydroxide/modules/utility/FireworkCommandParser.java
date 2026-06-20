package net.axther.hydroxide.modules.utility;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

final class FireworkCommandParser {

    static final int MAX_POWER = 127;
    static final int MAX_FIRE_COUNT = 16;

    private FireworkCommandParser() {
    }

    static Optional<Request> parse(List<String> args) {
        if (args.isEmpty() || args.size() > 2) {
            return Optional.empty();
        }

        String action = args.getFirst().toLowerCase(Locale.ROOT);
        return switch (action) {
            case "power" -> parseBoundedAmount(args, 1, 0, MAX_POWER)
                    .map(amount -> new Request(Action.POWER, amount));
            case "clear", "reset" -> args.size() == 1
                    ? Optional.of(new Request(Action.CLEAR, 0))
                    : Optional.empty();
            case "fire", "launch" -> parseBoundedAmount(args, 1, 1, MAX_FIRE_COUNT)
                    .map(amount -> new Request(Action.FIRE, amount));
            default -> Optional.empty();
        };
    }

    private static Optional<Integer> parseBoundedAmount(List<String> args, int defaultValue, int minimum, int maximum) {
        if (args.size() == 1) {
            return Optional.of(defaultValue);
        }
        try {
            int value = Integer.parseInt(args.get(1));
            return value >= minimum && value <= maximum ? Optional.of(value) : Optional.empty();
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }
    }

    enum Action {
        POWER,
        CLEAR,
        FIRE
    }

    record Request(Action action, int amount) {
    }
}
