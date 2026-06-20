package net.axther.hydroxide.modules.utility;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

final class UnbreakableCommandParser {

    private UnbreakableCommandParser() {
    }

    static Optional<Request> parse(List<String> args) {
        if (args.size() > 2) {
            return Optional.empty();
        }
        if (args.isEmpty()) {
            return Optional.of(new Request(Optional.empty(), Optional.empty()));
        }

        if (args.size() == 1) {
            StateInput state = state(args.getFirst());
            if (state == StateInput.ENABLE) {
                return Optional.of(new Request(Optional.empty(), Optional.of(true)));
            }
            if (state == StateInput.DISABLE) {
                return Optional.of(new Request(Optional.empty(), Optional.of(false)));
            }
            if (state == StateInput.TOGGLE) {
                return Optional.of(new Request(Optional.empty(), Optional.empty()));
            }
            return Optional.of(new Request(Optional.of(args.getFirst()), Optional.empty()));
        }

        StateInput state = state(args.get(1));
        if (state == StateInput.UNKNOWN) {
            return Optional.empty();
        }
        return Optional.of(new Request(Optional.of(args.getFirst()), switch (state) {
            case ENABLE -> Optional.of(true);
            case DISABLE -> Optional.of(false);
            case TOGGLE -> Optional.empty();
            case UNKNOWN -> throw new IllegalStateException("unknown state was already rejected");
        }));
    }

    private static StateInput state(String input) {
        return switch (input.toLowerCase(Locale.ROOT)) {
            case "true", "on", "yes", "enable", "enabled" -> StateInput.ENABLE;
            case "false", "off", "no", "disable", "disabled" -> StateInput.DISABLE;
            case "toggle" -> StateInput.TOGGLE;
            default -> StateInput.UNKNOWN;
        };
    }

    private enum StateInput {
        ENABLE,
        DISABLE,
        TOGGLE,
        UNKNOWN
    }

    record Request(Optional<String> target, Optional<Boolean> state) {
    }
}
