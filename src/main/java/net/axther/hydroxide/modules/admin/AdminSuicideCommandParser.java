package net.axther.hydroxide.modules.admin;

import java.util.List;
import java.util.Optional;

final class AdminSuicideCommandParser {

    private AdminSuicideCommandParser() {
    }

    static Optional<Request> parse(List<String> args) {
        if (args.isEmpty()) {
            return Optional.of(new Request(Optional.empty(), false));
        }
        if (args.size() == 1) {
            if (isSilent(args.getFirst())) {
                return Optional.of(new Request(Optional.empty(), true));
            }
            if (args.getFirst().startsWith("-")) {
                return Optional.empty();
            }
            return Optional.of(new Request(Optional.of(args.getFirst()), false));
        }
        if (args.size() == 2 && !args.getFirst().startsWith("-") && isSilent(args.get(1))) {
            return Optional.of(new Request(Optional.of(args.getFirst()), true));
        }
        return Optional.empty();
    }

    private static boolean isSilent(String input) {
        return input.equalsIgnoreCase("-s");
    }

    record Request(Optional<String> targetName, boolean silent) {
    }
}
