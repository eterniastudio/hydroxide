package net.axther.hydroxide.modules.admin;

import java.util.List;
import java.util.Optional;

final class AdminSameIpCommandParser {

    private AdminSameIpCommandParser() {
    }

    static Optional<Request> parse(List<String> args) {
        if (args.isEmpty()) {
            return Optional.of(new Request(Optional.empty()));
        }
        if (args.size() != 1 || args.getFirst().isBlank()) {
            return Optional.empty();
        }
        return Optional.of(new Request(Optional.of(args.getFirst().trim())));
    }

    record Request(Optional<String> query) {
    }
}
