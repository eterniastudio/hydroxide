package net.axther.hydroxide.modules.proxy;

import java.util.List;
import java.util.Optional;

final class ProxySendAllCommandParser {

    private ProxySendAllCommandParser() {
    }

    static Optional<Request> parse(List<String> args) {
        if (args.size() != 1 || args.getFirst().isBlank() || args.getFirst().startsWith("-")) {
            return Optional.empty();
        }
        return Optional.of(new Request(args.getFirst()));
    }

    record Request(String server) {
    }
}
