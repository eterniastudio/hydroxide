package net.axther.hydroxide.modules.utility;

import org.bukkit.inventory.ItemFlag;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

final class HideFlagsCommandParser {

    private HideFlagsCommandParser() {
    }

    static Optional<Request> parse(List<String> args) {
        if (args.isEmpty()) {
            return Optional.empty();
        }
        String first = normalize(args.getFirst());
        if (isClear(first)) {
            return args.size() == 1
                    ? Optional.of(new Request(Action.CLEAR, Set.of()))
                    : Optional.empty();
        }
        if (first.equals("ALL")) {
            return args.size() == 1
                    ? Optional.of(new Request(Action.ADD, Set.of(ItemFlag.values())))
                    : Optional.empty();
        }

        EnumSet<ItemFlag> flags = EnumSet.noneOf(ItemFlag.class);
        for (String arg : args) {
            Optional<ItemFlag> flag = flag(arg);
            if (flag.isEmpty()) {
                return Optional.empty();
            }
            flags.add(flag.get());
        }
        return flags.isEmpty()
                ? Optional.empty()
                : Optional.of(new Request(Action.ADD, Set.copyOf(flags)));
    }

    private static Optional<ItemFlag> flag(String input) {
        String normalized = normalize(input);
        return Arrays.stream(ItemFlag.values())
                .filter(flag -> flag.name().equals(normalized))
                .findFirst();
    }

    private static String normalize(String input) {
        return input.trim().replace('-', '_').toUpperCase(Locale.ROOT);
    }

    private static boolean isClear(String input) {
        return switch (input) {
            case "CLEAR", "RESET", "REMOVEALL" -> true;
            default -> false;
        };
    }

    enum Action {
        ADD,
        CLEAR
    }

    record Request(Action action, Set<ItemFlag> flags) {
    }
}
