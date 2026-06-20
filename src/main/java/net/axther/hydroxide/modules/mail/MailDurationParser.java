package net.axther.hydroxide.modules.mail;

import java.time.Duration;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class MailDurationParser {

    private static final Pattern PART = Pattern.compile("(\\d+)([smhdwMy])");

    private MailDurationParser() {
    }

    static Optional<Duration> parse(String input) {
        if (input == null || input.isBlank()) {
            return Optional.empty();
        }
        Matcher matcher = PART.matcher(input);
        int consumed = 0;
        Duration duration = Duration.ZERO;
        while (matcher.find()) {
            if (matcher.start() != consumed) {
                return Optional.empty();
            }
            long amount;
            try {
                amount = Long.parseLong(matcher.group(1));
            } catch (NumberFormatException exception) {
                return Optional.empty();
            }
            try {
                duration = duration.plus(switch (matcher.group(2)) {
                    case "s" -> Duration.ofSeconds(amount);
                    case "m" -> Duration.ofMinutes(amount);
                    case "h" -> Duration.ofHours(amount);
                    case "d" -> Duration.ofDays(amount);
                    case "w" -> Duration.ofDays(amount * 7L);
                    case "M" -> Duration.ofDays(amount * 30L);
                    case "y" -> Duration.ofDays(amount * 365L);
                    default -> Duration.ZERO;
                });
            } catch (ArithmeticException exception) {
                return Optional.empty();
            }
            consumed = matcher.end();
        }
        if (consumed != input.length() || duration.isZero() || duration.isNegative()) {
            return Optional.empty();
        }
        return Optional.of(duration);
    }
}
