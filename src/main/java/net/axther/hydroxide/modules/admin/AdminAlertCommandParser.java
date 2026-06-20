package net.axther.hydroxide.modules.admin;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

final class AdminAlertCommandParser {

    private AdminAlertCommandParser() {
    }

    static Optional<Request> parse(List<String> args) {
        if (args.isEmpty()) {
            return Optional.empty();
        }
        Action action;
        try {
            action = Action.valueOf(args.getFirst().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }

        List<String> values = new ArrayList<>(args.subList(1, args.size()));
        boolean silent = values.removeIf("-s"::equalsIgnoreCase);
        return switch (action) {
            case ADD -> parseAdd(values, silent);
            case LIST -> values.isEmpty()
                    ? Optional.of(new Request(action, Optional.empty(), Optional.empty(), silent))
                    : Optional.empty();
            case REMOVE -> values.size() == 1
                    ? Optional.of(new Request(action, Optional.of(values.getFirst()), Optional.empty(), silent))
                    : Optional.empty();
        };
    }

    private static Optional<Request> parseAdd(List<String> values, boolean silent) {
        if (values.isEmpty()) {
            return Optional.empty();
        }
        String target = values.getFirst();
        String reason = values.size() > 1 ? String.join(" ", values.subList(1, values.size())).trim() : "";
        return Optional.of(new Request(Action.ADD, Optional.of(target),
                reason.isBlank() ? Optional.empty() : Optional.of(reason), silent));
    }

    enum Action {
        ADD,
        LIST,
        REMOVE
    }

    record Request(Action action, Optional<String> playerName, Optional<String> reason, boolean silent) {
    }
}
