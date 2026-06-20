package net.axther.hydroxide.modules.utility;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalInt;

final class ItemEnchantCommandParser {

    private ItemEnchantCommandParser() {
    }

    static Optional<Request> parse(List<String> args) {
        if (args.isEmpty()) {
            return Optional.empty();
        }

        String first = args.getFirst().toLowerCase(Locale.ROOT);
        if (isClear(first)) {
            return args.size() == 1
                    ? Optional.of(new Request(Action.CLEAR, Optional.empty(), OptionalInt.empty()))
                    : Optional.empty();
        }

        if (isRemove(first)) {
            return args.size() == 2
                    ? Optional.of(new Request(Action.REMOVE, Optional.of(args.get(1)), OptionalInt.empty()))
                    : Optional.empty();
        }

        if (first.equals("add")) {
            if (args.size() < 2 || args.size() > 3) {
                return Optional.empty();
            }
            return add(args.get(1), args.size() == 3 ? args.get(2) : "1");
        }

        if (args.size() > 2) {
            return Optional.empty();
        }
        return add(args.getFirst(), args.size() == 2 ? args.get(1) : "1");
    }

    private static Optional<Request> add(String enchantment, String levelInput) {
        try {
            int level = Integer.parseInt(levelInput);
            if (level < 1) {
                return Optional.empty();
            }
            return Optional.of(new Request(Action.ADD, Optional.of(enchantment), OptionalInt.of(level)));
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }
    }

    private static boolean isRemove(String input) {
        return switch (input) {
            case "remove", "delete", "del" -> true;
            default -> false;
        };
    }

    private static boolean isClear(String input) {
        return switch (input) {
            case "clear", "clearall", "removeall" -> true;
            default -> false;
        };
    }

    enum Action {
        ADD,
        REMOVE,
        CLEAR
    }

    record Request(Action action, Optional<String> enchantment, OptionalInt level) {
    }
}
