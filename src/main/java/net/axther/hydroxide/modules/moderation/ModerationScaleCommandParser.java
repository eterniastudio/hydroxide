package net.axther.hydroxide.modules.moderation;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalDouble;

final class ModerationScaleCommandParser {

    private ModerationScaleCommandParser() {
    }

    static Optional<Request> parse(List<String> args) {
        List<String> values = new ArrayList<>(args);
        boolean silent = values.removeIf("-s"::equalsIgnoreCase);
        if (values.isEmpty()) {
            return Optional.empty();
        }
        Optional<Action> action = action(values.getFirst());
        if (action.isEmpty()) {
            return Optional.empty();
        }
        Action resolvedAction = action.orElseThrow();
        return resolvedAction == Action.CLEAR ? clear(values, silent) : amount(resolvedAction, values, silent);
    }

    private static Optional<Request> amount(Action action, List<String> values, boolean silent) {
        if (values.size() == 2) {
            OptionalDouble amount = amount(values.get(1));
            return amount.isPresent()
                    ? Optional.of(new Request(action, Optional.empty(), amount, silent))
                    : Optional.empty();
        }
        if (values.size() == 3) {
            OptionalDouble amount = amount(values.get(2));
            return amount.isPresent()
                    ? Optional.of(new Request(action, Optional.of(values.get(1)), amount, silent))
                    : Optional.empty();
        }
        return Optional.empty();
    }

    private static Optional<Request> clear(List<String> values, boolean silent) {
        if (values.size() == 1) {
            return Optional.of(new Request(Action.CLEAR, Optional.empty(), OptionalDouble.empty(), silent));
        }
        if (values.size() == 2) {
            return Optional.of(new Request(Action.CLEAR, Optional.of(values.get(1)), OptionalDouble.empty(), silent));
        }
        return Optional.empty();
    }

    private static Optional<Action> action(String input) {
        return switch (input.toLowerCase(Locale.ROOT)) {
            case "set" -> Optional.of(Action.SET);
            case "add" -> Optional.of(Action.ADD);
            case "take", "remove" -> Optional.of(Action.TAKE);
            case "clear", "reset" -> Optional.of(Action.CLEAR);
            default -> Optional.empty();
        };
    }

    private static OptionalDouble amount(String input) {
        try {
            double amount = Double.parseDouble(input);
            return Double.isFinite(amount) && amount > 0.0D
                    ? OptionalDouble.of(amount)
                    : OptionalDouble.empty();
        } catch (NumberFormatException exception) {
            return OptionalDouble.empty();
        }
    }

    enum Action {
        SET,
        ADD,
        TAKE,
        CLEAR
    }

    record Request(Action action, Optional<String> targetName, OptionalDouble amount, boolean silent) {
    }
}
