package net.axther.hydroxide.modules.moderation;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

final class ModerationHungerCommandParser {

    private ModerationHungerCommandParser() {
    }

    static Optional<Request> parse(List<String> args) {
        if (args.size() != 2) {
            return Optional.empty();
        }
        try {
            int amount = Integer.parseInt(args.get(1));
            if (amount < 0 || amount > 20) {
                return Optional.empty();
            }
            return Optional.of(new Request(Target.from(args.getFirst()), amount));
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }
    }

    record Target(String name, boolean all) {
        static Target from(String input) {
            return switch (input.toLowerCase(Locale.ROOT)) {
                case "all", "*", "@a" -> new Target("all", true);
                default -> new Target(input, false);
            };
        }
    }

    record Request(Target target, int amount) {
    }
}
