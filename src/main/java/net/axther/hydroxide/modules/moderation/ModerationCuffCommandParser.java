package net.axther.hydroxide.modules.moderation;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

final class ModerationCuffCommandParser {

    private ModerationCuffCommandParser() {
    }

    static Optional<Request> parse(List<String> args) {
        List<String> values = new ArrayList<>(args);
        boolean silent = values.removeIf("-s"::equalsIgnoreCase);
        if (values.isEmpty() || values.size() > 2) {
            return Optional.empty();
        }
        String targetName = values.getFirst();
        if (values.size() == 1) {
            return Optional.of(new Request(targetName, State.TOGGLE, silent));
        }
        return state(values.get(1)).map(state -> new Request(targetName, state, silent));
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

    record Request(String targetName, State state, boolean silent) {
    }
}
