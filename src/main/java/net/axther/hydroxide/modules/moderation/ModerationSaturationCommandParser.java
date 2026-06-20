package net.axther.hydroxide.modules.moderation;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

final class ModerationSaturationCommandParser {

    private ModerationSaturationCommandParser() {
    }

    static Optional<Request> parse(List<String> args) {
        if (args.size() != 2) {
            return Optional.empty();
        }
        try {
            float amount = Float.parseFloat(args.get(1));
            if (!Float.isFinite(amount) || amount < 0.0F || amount > 20.0F) {
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

    record Request(Target target, float amount) {
    }
}
