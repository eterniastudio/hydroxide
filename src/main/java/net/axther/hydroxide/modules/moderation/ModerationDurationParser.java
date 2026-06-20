package net.axther.hydroxide.modules.moderation;

import java.time.Duration;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class ModerationDurationParser {

    private static final Pattern PART = Pattern.compile("(\\d+)([smhdw])");

    private ModerationDurationParser() {
    }

    static Optional<Duration> parse(String input) {
        if (input == null || input.isBlank()) {
            return Optional.empty();
        }
        Matcher matcher = PART.matcher(input.toLowerCase(java.util.Locale.ROOT));
        int consumed = 0;
        Duration duration = Duration.ZERO;
        while (matcher.find()) {
            if (matcher.start() != consumed) {
                return Optional.empty();
            }
            long amount = Long.parseLong(matcher.group(1));
            duration = duration.plus(switch (matcher.group(2)) {
                case "s" -> Duration.ofSeconds(amount);
                case "m" -> Duration.ofMinutes(amount);
                case "h" -> Duration.ofHours(amount);
                case "d" -> Duration.ofDays(amount);
                case "w" -> Duration.ofDays(amount * 7L);
                default -> Duration.ZERO;
            });
            consumed = matcher.end();
        }
        if (consumed != input.length() || duration.isZero() || duration.isNegative()) {
            return Optional.empty();
        }
        return Optional.of(duration);
    }
}
