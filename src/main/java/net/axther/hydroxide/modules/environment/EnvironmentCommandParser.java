package net.axther.hydroxide.modules.environment;

import java.util.Locale;
import java.util.Optional;

final class EnvironmentCommandParser {

    private EnvironmentCommandParser() {
    }

    static Optional<Long> time(String input) {
        String normalized = input.toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "day", "morning" -> Optional.of(1000L);
            case "noon", "midday" -> Optional.of(6000L);
            case "sunset", "dusk" -> Optional.of(12000L);
            case "night" -> Optional.of(13000L);
            case "midnight" -> Optional.of(18000L);
            case "sunrise", "dawn" -> Optional.of(23000L);
            default -> ticks(normalized);
        };
    }

    static Optional<EnvironmentWeatherMode> weather(String input) {
        return switch (input.toLowerCase(Locale.ROOT)) {
            case "clear", "sun", "sunny" -> Optional.of(EnvironmentWeatherMode.CLEAR);
            case "rain", "storm" -> Optional.of(EnvironmentWeatherMode.RAIN);
            case "thunder", "thunderstorm" -> Optional.of(EnvironmentWeatherMode.THUNDER);
            default -> Optional.empty();
        };
    }

    private static Optional<Long> ticks(String input) {
        try {
            long ticks = Long.parseLong(input);
            return ticks >= 0L && ticks <= 23999L ? Optional.of(ticks) : Optional.empty();
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }
    }
}
