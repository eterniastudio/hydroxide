package net.axther.hydroxide.modules.moderation;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalDouble;

final class ModerationMaxHealthCommandParser {

    private ModerationMaxHealthCommandParser() {
    }

    static Optional<Request> parse(List<String> args) {
        if (args.isEmpty()) {
            return Optional.empty();
        }
        Optional<Action> action = action(args.getFirst());
        if (action.isEmpty()) {
            return Optional.empty();
        }
        return action.get() == Action.CLEAR ? clear(args) : amount(action.get(), args);
    }

    private static Optional<Request> amount(Action action, List<String> args) {
        if (args.size() == 2) {
            OptionalDouble amount = amount(args.get(1));
            return amount.isPresent()
                    ? Optional.of(new Request(action, Optional.empty(), amount))
                    : Optional.empty();
        }
        if (args.size() == 3) {
            OptionalDouble amount = amount(args.get(2));
            return amount.isPresent()
                    ? Optional.of(new Request(action, Optional.of(args.get(1)), amount))
                    : Optional.empty();
        }
        return Optional.empty();
    }

    private static Optional<Request> clear(List<String> args) {
        if (args.size() == 1) {
            return Optional.of(new Request(Action.CLEAR, Optional.empty(), OptionalDouble.empty()));
        }
        if (args.size() == 2) {
            return Optional.of(new Request(Action.CLEAR, Optional.of(args.get(1)), OptionalDouble.empty()));
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

    record Request(Action action, Optional<String> targetName, OptionalDouble amount) {
    }
}
