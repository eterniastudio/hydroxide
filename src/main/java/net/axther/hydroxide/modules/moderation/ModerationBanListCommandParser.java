package net.axther.hydroxide.modules.moderation;

import java.util.List;
import java.util.Optional;

final class ModerationBanListCommandParser {

    private ModerationBanListCommandParser() {
    }

    static Optional<Request> parse(List<String> args) {
        if (args.isEmpty()) {
            return Optional.of(new Request(1));
        }
        if (args.size() != 1) {
            return Optional.empty();
        }
        try {
            int page = Integer.parseInt(args.getFirst());
            return page <= 0 ? Optional.empty() : Optional.of(new Request(page));
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }
    }

    record Request(int page) {
    }
}
