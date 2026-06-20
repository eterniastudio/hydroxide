package net.axther.hydroxide.modules.utility;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

final class PowerToolToggleParser {

    private PowerToolToggleParser() {
    }

    static Optional<State> parse(List<String> args) {
        if (args.isEmpty()) {
            return Optional.of(State.TOGGLE);
        }
        if (args.size() != 1) {
            return Optional.empty();
        }
        return switch (args.getFirst().toLowerCase(Locale.ROOT)) {
            case "on", "enable", "enabled", "true" -> Optional.of(State.ENABLED);
            case "off", "disable", "disabled", "false" -> Optional.of(State.DISABLED);
            case "toggle" -> Optional.of(State.TOGGLE);
            default -> Optional.empty();
        };
    }

    enum State {
        ENABLED,
        DISABLED,
        TOGGLE
    }
}
