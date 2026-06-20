package net.axther.hydroxide.modules.moderation;

import java.util.List;
import java.util.Optional;

final class ModerationEditWarningsCommandParser {

    private ModerationEditWarningsCommandParser() {
    }

    static Optional<Request> parse(List<String> args) {
        if (args.isEmpty()) {
            return Optional.empty();
        }
        String first = args.getFirst();
        if (first.equalsIgnoreCase("clearall")) {
            if (args.size() == 1 || (args.size() == 2 && args.get(1).equalsIgnoreCase("clear"))) {
                return Optional.of(new Request(Action.CLEAR_ALL, Optional.empty()));
            }
            return Optional.empty();
        }
        if (args.size() == 2 && args.get(1).equalsIgnoreCase("clear")) {
            return Optional.of(new Request(Action.CLEAR_PLAYER, Optional.of(first)));
        }
        return Optional.empty();
    }

    enum Action {
        CLEAR_PLAYER,
        CLEAR_ALL
    }

    record Request(Action action, Optional<String> targetName) {
    }
}
