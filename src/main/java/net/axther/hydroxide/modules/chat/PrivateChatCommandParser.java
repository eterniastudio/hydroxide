package net.axther.hydroxide.modules.chat;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

final class PrivateChatCommandParser {

    private PrivateChatCommandParser() {
    }

    static Optional<Request> parse(List<String> args) {
        if (args.size() > 1 || args.stream().anyMatch(String::isBlank)) {
            return Optional.empty();
        }
        if (args.isEmpty()) {
            return Optional.of(new Request(Action.STATUS, Optional.empty()));
        }
        String value = args.getFirst();
        if (value.toLowerCase(Locale.ROOT).equals("off")) {
            return Optional.of(new Request(Action.CLEAR, Optional.empty()));
        }
        return Optional.of(new Request(Action.FOCUS, Optional.of(value)));
    }

    enum Action {
        STATUS,
        CLEAR,
        FOCUS
    }

    record Request(Action action, Optional<String> targetName) {
    }
}
