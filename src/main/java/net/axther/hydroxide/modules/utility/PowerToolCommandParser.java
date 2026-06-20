package net.axther.hydroxide.modules.utility;

import net.axther.hydroxide.commands.CommandUtils;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

final class PowerToolCommandParser {

    private PowerToolCommandParser() {
    }

    static Optional<Request> parse(List<String> args) {
        if (args.isEmpty()) {
            return Optional.empty();
        }

        if (args.size() == 1 && isClearAlias(args.getFirst())) {
            return Optional.of(new Request(Action.CLEAR, Optional.empty()));
        }

        String command = CommandUtils.joinArgs(args.toArray(String[]::new), 0).trim();
        if (command.startsWith("/")) {
            command = command.substring(1).trim();
        }
        if (command.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(new Request(Action.BIND, Optional.of(command)));
    }

    private static boolean isClearAlias(String input) {
        return switch (input.toLowerCase(Locale.ROOT)) {
            case "clear", "remove", "none", "off" -> true;
            default -> false;
        };
    }

    enum Action {
        BIND,
        CLEAR
    }

    record Request(Action action, Optional<String> command) {
    }
}
