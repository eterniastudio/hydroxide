package net.axther.hydroxide.modules.motd;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

final class EditCustomTextCommandParser {

    private EditCustomTextCommandParser() {
    }

    static Optional<Request> parse(List<String> args) {
        if (args.isEmpty()) {
            return Optional.of(new Request(Action.LIST, Optional.empty(), List.of()));
        }
        Action action = Action.from(args.getFirst()).orElse(null);
        if (action == null) {
            return Optional.empty();
        }
        return switch (action) {
            case LIST, RELOAD -> args.size() == 1
                    ? Optional.of(new Request(action, Optional.empty(), List.of()))
                    : Optional.empty();
            case SHOW, DELETE, ENABLE, DISABLE -> args.size() == 2 && !args.get(1).isBlank()
                    ? Optional.of(new Request(action, Optional.of(args.get(1)), List.of()))
                    : Optional.empty();
            case SET -> parseSet(args);
        };
    }

    private static Optional<Request> parseSet(List<String> args) {
        if (args.size() < 3 || args.get(1).isBlank()) {
            return Optional.empty();
        }
        String joined = String.join(" ", args.subList(2, args.size())).trim();
        if (joined.isBlank()) {
            return Optional.empty();
        }
        List<String> lines = Arrays.stream(joined.split("\\|"))
                .map(String::trim)
                .filter(line -> !line.isBlank())
                .toList();
        return lines.isEmpty()
                ? Optional.empty()
                : Optional.of(new Request(Action.SET, Optional.of(args.get(1)), lines));
    }

    enum Action {
        LIST,
        SHOW,
        SET,
        DELETE,
        ENABLE,
        DISABLE,
        RELOAD;

        static Optional<Action> from(String input) {
            try {
                return Optional.of(Action.valueOf(input.toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException exception) {
                return Optional.empty();
            }
        }
    }

    record Request(Action action, Optional<String> name, List<String> lines) {
    }
}
