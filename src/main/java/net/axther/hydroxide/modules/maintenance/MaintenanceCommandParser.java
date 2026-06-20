package net.axther.hydroxide.modules.maintenance;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

final class MaintenanceCommandParser {

    private MaintenanceCommandParser() {
    }

    static Optional<Request> parse(List<String> args) {
        if (args.isEmpty()) {
            return Optional.of(new Request(Action.STATUS, Optional.empty()));
        }
        String mode = args.getFirst().toLowerCase(Locale.ROOT);
        Action action = switch (mode) {
            case "true", "on", "enable", "enabled" -> Action.ENABLE;
            case "false", "off", "disable", "disabled" -> Action.DISABLE;
            case "status", "info" -> Action.STATUS;
            default -> null;
        };
        if (action == null) {
            return Optional.empty();
        }
        String message = args.size() > 1 ? String.join(" ", args.subList(1, args.size())).trim() : "";
        return Optional.of(new Request(action, message.isBlank() ? Optional.empty() : Optional.of(message)));
    }

    enum Action {
        ENABLE,
        DISABLE,
        STATUS
    }

    record Request(Action action, Optional<String> message) {
    }
}
