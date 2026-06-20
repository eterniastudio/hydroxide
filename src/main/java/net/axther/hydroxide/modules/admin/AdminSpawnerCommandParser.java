package net.axther.hydroxide.modules.admin;

import java.util.List;
import java.util.Optional;

final class AdminSpawnerCommandParser {

    private AdminSpawnerCommandParser() {
    }

    static Optional<Request> parse(List<String> args) {
        if (args.isEmpty() || args.size() > 3 || args.getFirst().startsWith("-")) {
            return Optional.empty();
        }

        String entityName = args.getFirst();
        Optional<String> targetName = Optional.empty();
        boolean silent = false;

        if (args.size() >= 2) {
            String second = args.get(1);
            if (second.equalsIgnoreCase("-s")) {
                silent = true;
            } else if (second.startsWith("-")) {
                return Optional.empty();
            } else {
                targetName = Optional.of(second);
            }
        }

        if (args.size() == 3) {
            if (targetName.isEmpty() || silent || !args.get(2).equalsIgnoreCase("-s")) {
                return Optional.empty();
            }
            silent = true;
        }

        return Optional.of(new Request(entityName, targetName, silent));
    }

    record Request(String entityName, Optional<String> targetName, boolean silent) {
    }
}
