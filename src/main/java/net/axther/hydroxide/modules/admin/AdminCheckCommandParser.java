package net.axther.hydroxide.modules.admin;

import java.util.List;
import java.util.Optional;

final class AdminCheckCommandParser {

    private AdminCheckCommandParser() {
    }

    static Optional<Request> parse(List<String> args) {
        if (args.size() > 1) {
            return Optional.empty();
        }
        if (args.isEmpty() || args.getFirst().isBlank()) {
            return Optional.of(new Request(Optional.empty()));
        }
        return Optional.of(new Request(Optional.of(args.getFirst().trim().toLowerCase(java.util.Locale.ROOT))));
    }

    record Request(Optional<String> keyword) {
    }
}
