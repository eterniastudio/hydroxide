package net.axther.hydroxide.modules.admin;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;

final class CompassCommandParser {

    private CompassCommandParser() {
    }

    static Optional<Request> parse(List<String> args) {
        List<String> values = new ArrayList<>(args);
        boolean silent = false;
        if (!values.isEmpty() && isSilent(values.getLast())) {
            silent = true;
            values.removeLast();
        }

        if (values.isEmpty()) {
            return Optional.of(new Request(Mode.DIRECTION, Optional.empty(), Optional.empty(),
                    OptionalDouble.empty(), OptionalDouble.empty(), Optional.empty(), silent));
        }

        if (values.getFirst().equalsIgnoreCase("reset")) {
            if (values.size() == 1) {
                return Optional.of(new Request(Mode.RESET, Optional.empty(), Optional.empty(),
                        OptionalDouble.empty(), OptionalDouble.empty(), Optional.empty(), silent));
            }
            if (values.size() == 2 && !looksLikeFlag(values.get(1))) {
                return Optional.of(new Request(Mode.RESET, Optional.of(values.get(1)), Optional.empty(),
                        OptionalDouble.empty(), OptionalDouble.empty(), Optional.empty(), silent));
            }
            return Optional.empty();
        }

        if (values.size() == 1) {
            return looksLikeFlag(values.getFirst()) ? Optional.empty() : Optional.of(new Request(
                    Mode.PLAYER, Optional.of(values.getFirst()), Optional.empty(),
                    OptionalDouble.empty(), OptionalDouble.empty(), Optional.empty(), silent));
        }

        if (values.size() == 2) {
            if (parseDouble(values.get(1)).isPresent() || looksLikeFlag(values.getFirst()) || looksLikeFlag(values.get(1))) {
                return Optional.empty();
            }
            return Optional.of(new Request(Mode.PLAYER, Optional.of(values.getFirst()), Optional.of(values.get(1)),
                    OptionalDouble.empty(), OptionalDouble.empty(), Optional.empty(), silent));
        }

        if (values.size() == 3 || values.size() == 4) {
            if (looksLikeFlag(values.getFirst())) {
                return Optional.empty();
            }
            OptionalDouble x = parseDouble(values.get(1));
            OptionalDouble z = parseDouble(values.get(2));
            if (x.isEmpty() || z.isEmpty()) {
                return Optional.empty();
            }
            Optional<String> worldName = values.size() == 4 ? Optional.of(values.get(3)).filter(value -> !looksLikeFlag(value)) : Optional.empty();
            if (values.size() == 4 && worldName.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(new Request(Mode.COORDINATES, Optional.of(values.getFirst()), Optional.empty(),
                    x, z, worldName, silent));
        }

        return Optional.empty();
    }

    private static OptionalDouble parseDouble(String input) {
        try {
            return OptionalDouble.of(Double.parseDouble(input));
        } catch (NumberFormatException exception) {
            return OptionalDouble.empty();
        }
    }

    private static boolean isSilent(String input) {
        return input.equalsIgnoreCase("-s");
    }

    private static boolean looksLikeFlag(String input) {
        return input.startsWith("-") && parseDouble(input).isEmpty();
    }

    enum Mode {
        DIRECTION,
        PLAYER,
        COORDINATES,
        RESET
    }

    record Request(Mode mode, Optional<String> targetName, Optional<String> sourceName,
                   OptionalDouble x, OptionalDouble z, Optional<String> worldName, boolean silent) {
    }
}
