package net.axther.hydroxide.modules.moderation;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

final class ModerationPlayerCollisionCommandParser {

    private ModerationPlayerCollisionCommandParser() {
    }

    static Optional<Request> parse(List<String> args) {
        List<String> values = new ArrayList<>(args);
        boolean silent = values.removeIf("-s"::equalsIgnoreCase);
        if (values.isEmpty()) {
            return Optional.of(new Request(Optional.empty(), State.TOGGLE, silent));
        }
        if (values.size() == 1) {
            Optional<State> state = state(values.getFirst());
            return state.map(value -> new Request(Optional.empty(), value, silent))
                    .or(() -> Optional.of(new Request(Optional.of(values.getFirst()), State.TOGGLE, silent)));
        }
        if (values.size() == 2) {
            return state(values.get(1)).map(value -> new Request(Optional.of(values.getFirst()), value, silent));
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

    record Request(Optional<String> targetName, State state, boolean silent) {
    }
}
