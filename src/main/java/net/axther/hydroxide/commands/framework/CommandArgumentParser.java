package net.axther.hydroxide.commands.framework;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

public final class CommandArgumentParser {

    public Optional<Integer> integer(String input) {
        try {
            return Optional.of(Integer.parseInt(input));
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }
    }

    public Optional<Double> money(String input) {
        try {
            double value = Double.parseDouble(input);
            if (!Double.isFinite(value) || value < 0.0D) {
                return Optional.empty();
            }
            BigDecimal decimal = new BigDecimal(input);
            return decimal.scale() <= 2 ? Optional.of(value) : Optional.empty();
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }
    }

    public Optional<UUID> uuid(String input) {
        try {
            return Optional.of(UUID.fromString(input));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    public Optional<Duration> duration(String input) {
        if (input == null || input.length() < 2) {
            return Optional.empty();
        }
        String amount = input.substring(0, input.length() - 1);
        char unit = Character.toLowerCase(input.charAt(input.length() - 1));
        Optional<Integer> value = integer(amount);
        if (value.isEmpty() || value.get() < 0) {
            return Optional.empty();
        }
        return switch (unit) {
            case 's' -> Optional.of(Duration.ofSeconds(value.get()));
            case 'm' -> Optional.of(Duration.ofMinutes(value.get()));
            case 'h' -> Optional.of(Duration.ofHours(value.get()));
            case 'd' -> Optional.of(Duration.ofDays(value.get()));
            default -> Optional.empty();
        };
    }

    public <E extends Enum<E>> Optional<E> enumValue(Class<E> type, String input) {
        try {
            return Optional.of(Enum.valueOf(type, input.toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }
}
