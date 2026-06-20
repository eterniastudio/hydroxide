package net.axther.hydroxide.modules.moderation;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

final class ModerationEffectCommandParser {

    private static final Duration DEFAULT_DURATION = Duration.ofSeconds(60);

    private ModerationEffectCommandParser() {
    }

    static Optional<Request> parse(List<String> args) {
        if (args.size() < 2) {
            return Optional.empty();
        }

        Target target = Target.from(args.get(0));
        String effect = args.get(1).toLowerCase(Locale.ROOT);
        if (effect.equals("clear") || effect.equals("remove")) {
            return Optional.of(new Request(target, Action.CLEAR, Optional.empty(), DEFAULT_DURATION, 0, false));
        }

        Duration duration = DEFAULT_DURATION;
        int amplifier = 0;
        if (args.size() >= 3 && !args.get(2).equalsIgnoreCase("-visual")) {
            Optional<Duration> parsed = duration(args.get(2));
            if (parsed.isEmpty()) {
                return Optional.empty();
            }
            duration = parsed.get();
        }
        if (args.size() >= 4 && !args.get(3).equalsIgnoreCase("-visual")) {
            Optional<Integer> parsed = integer(args.get(3), 0, 255);
            if (parsed.isEmpty()) {
                return Optional.empty();
            }
            amplifier = parsed.get();
        }

        boolean particles = args.stream().anyMatch(token -> token.equalsIgnoreCase("-visual"));
        return Optional.of(new Request(target, Action.APPLY, Optional.of(effect), duration, amplifier, particles));
    }

    private static Optional<Duration> duration(String input) {
        try {
            long seconds = Long.parseLong(input);
            return seconds <= 0 || seconds > 86_400 ? Optional.empty() : Optional.of(Duration.ofSeconds(seconds));
        } catch (NumberFormatException ignored) {
            return ModerationDurationParser.parse(input)
                    .filter(duration -> !duration.isZero() && !duration.isNegative() && duration.compareTo(Duration.ofDays(1)) <= 0);
        }
    }

    private static Optional<Integer> integer(String input, int minimum, int maximum) {
        try {
            int value = Integer.parseInt(input);
            return value < minimum || value > maximum ? Optional.empty() : Optional.of(value);
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }
    }

    enum Action {
        APPLY,
        CLEAR
    }

    record Target(String name, boolean all) {
        static Target from(String input) {
            return switch (input.toLowerCase(Locale.ROOT)) {
                case "all", "*", "@a" -> new Target("all", true);
                default -> new Target(input, false);
            };
        }
    }

    record Request(Target target, Action action, Optional<String> effect, Duration duration, int amplifier, boolean particles) {
    }
}
