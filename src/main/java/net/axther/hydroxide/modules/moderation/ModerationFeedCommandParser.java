package net.axther.hydroxide.modules.moderation;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

final class ModerationFeedCommandParser {

    private ModerationFeedCommandParser() {
    }

    static Optional<Request> parse(List<String> args) {
        if (args.size() > 2) {
            return Optional.empty();
        }
        boolean silent = false;
        Optional<String> target = Optional.empty();
        for (String arg : args) {
            if (arg.equalsIgnoreCase("-s")) {
                silent = true;
            } else if (!arg.startsWith("-") && target.isEmpty()) {
                target = Optional.of(arg);
            } else {
                return Optional.empty();
            }
        }
        return Optional.of(new Request(target.map(Target::from).orElseGet(Target::self), silent));
    }

    record Target(Optional<String> name, boolean all) {
        static Target self() {
            return new Target(Optional.empty(), false);
        }

        static Target from(String input) {
            return switch (input.toLowerCase(Locale.ROOT)) {
                case "all", "*", "@a" -> new Target(Optional.of("all"), true);
                default -> new Target(Optional.of(input), false);
            };
        }
    }

    record Request(Target target, boolean silent) {
    }
}
