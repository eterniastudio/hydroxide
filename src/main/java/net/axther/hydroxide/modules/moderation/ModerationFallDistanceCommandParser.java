package net.axther.hydroxide.modules.moderation;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

final class ModerationFallDistanceCommandParser {

    private static final float MAX_DISTANCE = 10_000.0F;

    private ModerationFallDistanceCommandParser() {
    }

    static Optional<Request> parse(List<String> args) {
        if (args.size() != 2) {
            return Optional.empty();
        }
        try {
            float distance = Float.parseFloat(args.get(1));
            if (!Float.isFinite(distance) || distance < 0.0F || distance > MAX_DISTANCE) {
                return Optional.empty();
            }
            return Optional.of(new Request(Target.from(args.getFirst()), distance));
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

    record Request(Target target, float distance) {
    }
}
