package net.axther.hydroxide.modules.proxy;

import java.util.List;
import java.util.Optional;

final class ProxyServerCommandParser {

    private ProxyServerCommandParser() {
    }

    static Optional<Request> parse(List<String> args) {
        if (args.isEmpty() || args.size() > 3 || args.getFirst().isBlank() || args.getFirst().startsWith("-")) {
            return Optional.empty();
        }

        String server = args.getFirst();
        Optional<String> targetName = Optional.empty();
        boolean force = false;
        if (args.size() >= 2) {
            String second = args.get(1);
            if (second.equalsIgnoreCase("-f")) {
                force = true;
            } else if (second.startsWith("-")) {
                return Optional.empty();
            } else {
                targetName = Optional.of(second);
            }
        }
        if (args.size() == 3) {
            if (!args.get(2).equalsIgnoreCase("-f") || targetName.isEmpty()) {
                return Optional.empty();
            }
            force = true;
        }

        return Optional.of(new Request(server, targetName, force));
    }

    record Request(String server, Optional<String> targetName, boolean force) {
    }
}
