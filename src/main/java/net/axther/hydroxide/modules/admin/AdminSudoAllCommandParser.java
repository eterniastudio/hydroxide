package net.axther.hydroxide.modules.admin;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

final class AdminSudoAllCommandParser {

    private AdminSudoAllCommandParser() {
    }

    static Optional<Request> parse(List<String> args) {
        if (args.size() < 2) {
            return Optional.empty();
        }
        Optional<Mode> mode = Mode.from(args.getFirst());
        if (mode.isEmpty()) {
            return Optional.empty();
        }
        String value = String.join(" ", args.subList(1, args.size())).trim();
        if (value.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(new Request(mode.orElseThrow(), value));
    }

    enum Mode {
        CHAT,
        COMMAND;

        static Optional<Mode> from(String input) {
            return switch (input.toLowerCase(Locale.ROOT)) {
                case "chat", "say" -> Optional.of(CHAT);
                case "command", "cmd" -> Optional.of(COMMAND);
                default -> Optional.empty();
            };
        }
    }

    record Request(Mode mode, String value) {
    }
}
