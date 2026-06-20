package net.axther.hydroxide.modules.announcement;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

final class AnnouncementCommandParser {

    private static final Duration DEFAULT_ACTIONBAR_DURATION = Duration.ofSeconds(3);
    private static final Duration DEFAULT_BOSSBAR_DURATION = Duration.ofSeconds(6);
    private static final int DEFAULT_FADE_IN_TICKS = 10;
    private static final int DEFAULT_STAY_TICKS = 70;
    private static final int DEFAULT_FADE_OUT_TICKS = 20;

    private AnnouncementCommandParser() {
    }

    static Optional<TimedMessage> actionbar(List<String> args) {
        return timed(args, DEFAULT_ACTIONBAR_DURATION);
    }

    static Optional<TimedMessage> bossbar(List<String> args) {
        return timed(args, DEFAULT_BOSSBAR_DURATION);
    }

    static Optional<TitleMessage> title(List<String> args) {
        if (args.size() < 2) {
            return Optional.empty();
        }

        Target target = target(args.getFirst());
        int fadeInTicks = DEFAULT_FADE_IN_TICKS;
        int stayTicks = DEFAULT_STAY_TICKS;
        int fadeOutTicks = DEFAULT_FADE_OUT_TICKS;
        List<String> messageTokens = new ArrayList<>();

        for (int index = 1; index < args.size(); index++) {
            String token = args.get(index);
            if (token.startsWith("-in:")) {
                Optional<Integer> parsed = positiveInt(token.substring("-in:".length()), 1, 200);
                if (parsed.isEmpty()) {
                    return Optional.empty();
                }
                fadeInTicks = parsed.get();
            } else if (token.startsWith("-keep:")) {
                Optional<Integer> parsed = positiveInt(token.substring("-keep:".length()), 1, 6000);
                if (parsed.isEmpty()) {
                    return Optional.empty();
                }
                stayTicks = parsed.get();
            } else if (token.startsWith("-out:")) {
                Optional<Integer> parsed = positiveInt(token.substring("-out:".length()), 1, 200);
                if (parsed.isEmpty()) {
                    return Optional.empty();
                }
                fadeOutTicks = parsed.get();
            } else {
                messageTokens.add(token);
            }
        }

        String message = String.join(" ", messageTokens).trim();
        if (message.isBlank()) {
            return Optional.empty();
        }
        String[] parts = message.split("\\\\n", 2);
        return Optional.of(new TitleMessage(
                target,
                parts[0].trim(),
                parts.length > 1 ? parts[1].trim() : "",
                fadeInTicks,
                stayTicks,
                fadeOutTicks
        ));
    }

    static Optional<FormattedMessage> tellRaw(List<String> args) {
        if (args.size() < 2) {
            return Optional.empty();
        }

        Target target = target(args.getFirst());
        String message = String.join(" ", args.subList(1, args.size())).trim();
        return message.isBlank() ? Optional.empty() : Optional.of(new FormattedMessage(target, message));
    }

    private static Optional<TimedMessage> timed(List<String> args, Duration defaultDuration) {
        if (args.size() < 2) {
            return Optional.empty();
        }

        Target target = target(args.getFirst());
        Duration duration = defaultDuration;
        List<String> messageTokens = new ArrayList<>();

        for (int index = 1; index < args.size(); index++) {
            String token = args.get(index);
            if (token.startsWith("-s:") || token.startsWith("-sec:")) {
                int offset = token.startsWith("-s:") ? "-s:".length() : "-sec:".length();
                Optional<Integer> seconds = positiveInt(token.substring(offset), 1, 300);
                if (seconds.isEmpty()) {
                    return Optional.empty();
                }
                duration = Duration.ofSeconds(seconds.get());
            } else {
                messageTokens.add(token);
            }
        }

        String message = String.join(" ", messageTokens).trim();
        return message.isBlank() ? Optional.empty() : Optional.of(new TimedMessage(target, message, duration));
    }

    private static Optional<Integer> positiveInt(String input, int minimum, int maximum) {
        try {
            int value = Integer.parseInt(input);
            if (value < minimum || value > maximum) {
                return Optional.empty();
            }
            return Optional.of(value);
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }
    }

    private static Target target(String input) {
        String normalized = input.toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "all", "*", "@a" -> new Target("all", true);
            default -> new Target(input, false);
        };
    }

    record Target(String name, boolean all) {
    }

    record FormattedMessage(Target target, String message) {
    }

    record TimedMessage(Target target, String message, Duration duration) {
    }

    record TitleMessage(Target target, String title, String subtitle, int fadeInTicks, int stayTicks, int fadeOutTicks) {
    }
}
