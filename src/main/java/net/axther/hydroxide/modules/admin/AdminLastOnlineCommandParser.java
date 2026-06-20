package net.axther.hydroxide.modules.admin;

import java.util.List;
import java.util.Optional;

final class AdminLastOnlineCommandParser {

    private AdminLastOnlineCommandParser() {
    }

    static Optional<Request> parse(List<String> args) {
        if (args.isEmpty()) {
            return Optional.of(new Request(1));
        }
        if (args.size() != 1) {
            return Optional.empty();
        }

        String token = args.getFirst().trim();
        if (token.toLowerCase(java.util.Locale.ROOT).startsWith("-p:")) {
            token = token.substring(3);
        }

        try {
            int page = Integer.parseInt(token);
            return page > 0 ? Optional.of(new Request(page)) : Optional.empty();
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }
    }

    record Request(int page) {
    }
}
