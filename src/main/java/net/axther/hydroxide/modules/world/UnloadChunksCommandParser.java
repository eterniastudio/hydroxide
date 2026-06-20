package net.axther.hydroxide.modules.world;

import java.util.List;
import java.util.Optional;

final class UnloadChunksCommandParser {

    private UnloadChunksCommandParser() {
    }

    static Optional<Request> parse(List<String> args) {
        if (args.isEmpty()) {
            return Optional.of(new Request(false));
        }
        if (args.size() == 1 && args.getFirst().equalsIgnoreCase("-f")) {
            return Optional.of(new Request(true));
        }
        return Optional.empty();
    }

    record Request(boolean forced) {
    }
}
