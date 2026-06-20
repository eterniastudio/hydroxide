package net.axther.hydroxide.modules.moderation;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

final class ModerationKickCommandParser {

    private ModerationKickCommandParser() {
    }

    static Optional<Request> parse(List<String> args) {
        if (args.isEmpty()) {
            return Optional.empty();
        }
        boolean silent = false;
        List<String> reasonParts = new ArrayList<>();
        for (String arg : args.subList(1, args.size())) {
            if (arg.equalsIgnoreCase("-s")) {
                silent = true;
            } else {
                reasonParts.add(arg);
            }
        }
        String reason = String.join(" ", reasonParts).trim();
        return Optional.of(new Request(
                Target.from(args.getFirst()),
                reason.isEmpty() ? Optional.empty() : Optional.of(reason),
                silent
        ));
    }

    record Target(String name, boolean all) {
        static Target from(String input) {
            return switch (input.toLowerCase(Locale.ROOT)) {
                case "all", "*", "@a" -> new Target("all", true);
                default -> new Target(input, false);
            };
        }
    }

    record Request(Target target, Optional<String> reason, boolean silent) {
    }
}
