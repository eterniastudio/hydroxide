package net.axther.hydroxide.modules.admin;

import java.util.List;
import java.util.Optional;

final class EnderChestCommandParser {

    private EnderChestCommandParser() {
    }

    static Optional<Request> parse(List<String> args) {
        if (args.isEmpty()) {
            return Optional.of(new Request(Optional.empty(), Optional.empty(), false));
        }
        if (args.size() == 1) {
            if (isSilent(args.getFirst())) {
                return Optional.of(new Request(Optional.empty(), Optional.empty(), true));
            }
            if (args.getFirst().startsWith("-")) {
                return Optional.empty();
            }
            return Optional.of(new Request(Optional.of(args.getFirst()), Optional.empty(), false));
        }
        if (args.size() == 2) {
            if (isSilent(args.get(1)) && !args.getFirst().startsWith("-")) {
                return Optional.of(new Request(Optional.of(args.getFirst()), Optional.empty(), true));
            }
            if (args.get(0).startsWith("-") || args.get(1).startsWith("-")) {
                return Optional.empty();
            }
            return Optional.of(new Request(Optional.of(args.getFirst()), Optional.of(args.get(1)), false));
        }
        if (args.size() == 3 && isSilent(args.get(2)) && !args.get(0).startsWith("-") && !args.get(1).startsWith("-")) {
            return Optional.of(new Request(Optional.of(args.getFirst()), Optional.of(args.get(1)), true));
        }
        return Optional.empty();
    }

    private static boolean isSilent(String input) {
        return input.equalsIgnoreCase("-s");
    }

    record Request(Optional<String> sourceName, Optional<String> viewerName, boolean silent) {
    }
}
