package net.axther.hydroxide.modules.admin;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalInt;

final class FindBiomeCommandParser {

    private FindBiomeCommandParser() {
    }

    static Optional<Request> parse(List<String> args) {
        if (args.isEmpty()) {
            return Optional.empty();
        }
        String first = args.getFirst();
        if (first.equalsIgnoreCase("stop")) {
            return args.size() == 1
                    ? Optional.of(new Request(Action.STOP, Optional.empty(), OptionalInt.empty()))
                    : Optional.empty();
        }
        if (first.equalsIgnoreCase("stopall")) {
            return args.size() == 1
                    ? Optional.of(new Request(Action.STOP_ALL, Optional.empty(), OptionalInt.empty()))
                    : Optional.empty();
        }
        if (first.startsWith("-")) {
            return Optional.empty();
        }

        OptionalInt radius = OptionalInt.empty();
        if (args.size() == 2) {
            String second = args.get(1).toLowerCase(Locale.ROOT);
            if (!second.startsWith("-r:")) {
                return Optional.empty();
            }
            radius = parsePositiveInt(args.get(1).substring(3));
            if (radius.isEmpty()) {
                return Optional.empty();
            }
        } else if (args.size() > 2) {
            return Optional.empty();
        }
        return Optional.of(new Request(Action.SEARCH, Optional.of(first), radius));
    }

    private static OptionalInt parsePositiveInt(String input) {
        try {
            int value = Integer.parseInt(input);
            return value > 0 ? OptionalInt.of(value) : OptionalInt.empty();
        } catch (NumberFormatException exception) {
            return OptionalInt.empty();
        }
    }

    enum Action {
        SEARCH,
        STOP,
        STOP_ALL
    }

    record Request(Action action, Optional<String> biomeName, OptionalInt radius) {
    }
}
