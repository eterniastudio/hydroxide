package net.axther.hydroxide.modules.admin;

import java.util.List;
import java.util.Optional;

final class AdminCheckAccountCommandParser {

    private AdminCheckAccountCommandParser() {
    }

    static Optional<Request> parse(List<String> args) {
        if (args.size() != 1 || args.getFirst().isBlank()) {
            return Optional.empty();
        }
        return Optional.of(new Request(args.getFirst().trim()));
    }

    record Request(String query) {
    }
}
