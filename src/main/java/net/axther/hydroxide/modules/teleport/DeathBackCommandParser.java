package net.axther.hydroxide.modules.teleport;

import java.util.List;
import java.util.Optional;

final class DeathBackCommandParser {

    private DeathBackCommandParser() {
    }

    static Optional<Request> parse(List<String> args) {
        if (args.isEmpty()) {
            return Optional.of(new Request(Optional.empty(), false));
        }
        if (args.size() == 1) {
            String first = args.getFirst();
            if (isSilent(first)) {
                return Optional.of(new Request(Optional.empty(), true));
            }
            if (first.startsWith("-")) {
                return Optional.empty();
            }
            return Optional.of(new Request(Optional.of(first), false));
        }
        if (args.size() == 2) {
            String first = args.getFirst();
            String second = args.get(1);
            if (isSilent(first) && !second.startsWith("-")) {
                return Optional.of(new Request(Optional.of(second), true));
            }
            if (!first.startsWith("-") && isSilent(second)) {
                return Optional.of(new Request(Optional.of(first), true));
            }
        }
        return Optional.empty();
    }

    private static boolean isSilent(String input) {
        return input.equalsIgnoreCase("-s");
    }

    record Request(Optional<String> targetName, boolean silent) {
    }
}
