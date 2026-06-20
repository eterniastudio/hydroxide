package net.axther.hydroxide.modules.admin;

import java.util.List;
import java.util.Optional;

final class AdminUncondenseCommandParser {

    private AdminUncondenseCommandParser() {
    }

    static Optional<Request> parse(List<String> args) {
        if (args.isEmpty()) {
            return Optional.of(new Request(Optional.empty(), Optional.empty(), false));
        }
        if (args.size() > 3) {
            return Optional.empty();
        }

        boolean silent = false;
        Optional<String> materialName = Optional.empty();
        Optional<String> targetName = Optional.empty();

        String first = args.getFirst();
        if (isSilent(first)) {
            silent = true;
        } else if (first.startsWith("-")) {
            return Optional.empty();
        } else if (!first.equalsIgnoreCase("all")) {
            materialName = Optional.of(first);
        }

        if (args.size() >= 2) {
            String second = args.get(1);
            if (isSilent(second)) {
                if (silent) {
                    return Optional.empty();
                }
                silent = true;
            } else if (second.startsWith("-")) {
                return Optional.empty();
            } else {
                targetName = Optional.of(second);
            }
        }

        if (args.size() == 3) {
            if (targetName.isEmpty() || silent || !isSilent(args.get(2))) {
                return Optional.empty();
            }
            silent = true;
        }

        return Optional.of(new Request(materialName, targetName, silent));
    }

    private static boolean isSilent(String input) {
        return input.equalsIgnoreCase("-s");
    }

    record Request(Optional<String> materialName, Optional<String> targetName, boolean silent) {
    }
}
