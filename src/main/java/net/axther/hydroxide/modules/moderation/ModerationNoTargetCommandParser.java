package net.axther.hydroxide.modules.moderation;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

final class ModerationNoTargetCommandParser {

    private ModerationNoTargetCommandParser() {
    }

    static Optional<Request> parse(List<String> args) {
        if (args.isEmpty()) {
            return Optional.of(new Request(Optional.empty(), State.TOGGLE));
        }
        if (args.size() == 1) {
            Optional<State> state = state(args.getFirst());
            return state.map(value -> new Request(Optional.empty(), value))
                    .or(() -> Optional.of(new Request(Optional.of(args.getFirst()), State.TOGGLE)));
        }
        if (args.size() == 2) {
            Optional<State> state = state(args.get(1));
            return state.map(value -> new Request(Optional.of(args.getFirst()), value));
        }
        return Optional.empty();
    }

    private static Optional<State> state(String input) {
        return switch (input.toLowerCase(Locale.ROOT)) {
            case "true", "on", "enable", "enabled" -> Optional.of(State.ENABLED);
            case "false", "off", "disable", "disabled" -> Optional.of(State.DISABLED);
            case "toggle" -> Optional.of(State.TOGGLE);
            default -> Optional.empty();
        };
    }

    enum State {
        ENABLED,
        DISABLED,
        TOGGLE
    }

    record Request(Optional<String> targetName, State state) {
    }
}
