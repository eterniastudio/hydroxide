package net.axther.hydroxide.modules.admin;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalDouble;

final class AdminLaunchCommandParser {

    private AdminLaunchCommandParser() {
    }

    static Optional<Request> parse(List<String> args) {
        Optional<String> targetName = Optional.empty();
        OptionalDouble power = OptionalDouble.empty();
        OptionalDouble angle = OptionalDouble.empty();
        OptionalDouble directionDegrees = OptionalDouble.empty();
        Optional<LocationTarget> locationTarget = Optional.empty();
        boolean noDamage = false;
        boolean silent = false;

        for (String raw : args) {
            String token = raw.trim();
            if (token.isBlank()) {
                return Optional.empty();
            }
            String normalized = token.toLowerCase(Locale.ROOT);
            if (normalized.equals("-nodamage")) {
                noDamage = true;
            } else if (normalized.equals("-s")) {
                silent = true;
            } else if (normalized.startsWith("p:")) {
                if (power.isPresent()) {
                    return Optional.empty();
                }
                OptionalDouble parsed = positiveDouble(token.substring(2));
                if (parsed.isEmpty()) {
                    return Optional.empty();
                }
                power = parsed;
            } else if (normalized.startsWith("a:")) {
                if (angle.isPresent()) {
                    return Optional.empty();
                }
                OptionalDouble parsed = boundedDouble(token.substring(2), -90.0D, 90.0D);
                if (parsed.isEmpty()) {
                    return Optional.empty();
                }
                angle = parsed;
            } else if (normalized.startsWith("d:")) {
                if (directionDegrees.isPresent()) {
                    return Optional.empty();
                }
                OptionalDouble parsed = direction(token.substring(2));
                if (parsed.isEmpty()) {
                    return Optional.empty();
                }
                directionDegrees = parsed;
            } else if (normalized.startsWith("loc:")) {
                if (locationTarget.isPresent()) {
                    return Optional.empty();
                }
                Optional<LocationTarget> parsed = location(token.substring(4));
                if (parsed.isEmpty()) {
                    return Optional.empty();
                }
                locationTarget = parsed;
            } else if (token.startsWith("-") || targetName.isPresent()) {
                return Optional.empty();
            } else {
                targetName = Optional.of(token);
            }
        }

        return Optional.of(new Request(targetName, power, angle, directionDegrees, locationTarget, noDamage, silent));
    }

    private static OptionalDouble positiveDouble(String input) {
        OptionalDouble parsed = finiteDouble(input);
        return parsed.isPresent() && parsed.getAsDouble() > 0.0D ? parsed : OptionalDouble.empty();
    }

    private static OptionalDouble boundedDouble(String input, double min, double max) {
        OptionalDouble parsed = finiteDouble(input);
        if (parsed.isEmpty()) {
            return OptionalDouble.empty();
        }
        double value = parsed.getAsDouble();
        return value >= min && value <= max ? parsed : OptionalDouble.empty();
    }

    private static OptionalDouble direction(String input) {
        return switch (input.trim().toLowerCase(Locale.ROOT)) {
            case "south", "s" -> OptionalDouble.of(0.0D);
            case "west", "w" -> OptionalDouble.of(90.0D);
            case "north", "n" -> OptionalDouble.of(180.0D);
            case "east", "e" -> OptionalDouble.of(-90.0D);
            default -> finiteDouble(input);
        };
    }

    private static Optional<LocationTarget> location(String input) {
        String[] parts = input.split(":", -1);
        if (parts.length != 3) {
            return Optional.empty();
        }
        OptionalDouble x = finiteDouble(parts[0]);
        OptionalDouble y = finiteDouble(parts[1]);
        OptionalDouble z = finiteDouble(parts[2]);
        if (x.isEmpty() || y.isEmpty() || z.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new LocationTarget(x.getAsDouble(), y.getAsDouble(), z.getAsDouble()));
    }

    private static OptionalDouble finiteDouble(String input) {
        try {
            double value = Double.parseDouble(input.trim());
            return Double.isFinite(value) ? OptionalDouble.of(value) : OptionalDouble.empty();
        } catch (NumberFormatException exception) {
            return OptionalDouble.empty();
        }
    }

    record Request(Optional<String> targetName, OptionalDouble power, OptionalDouble angle,
                   OptionalDouble directionDegrees, Optional<LocationTarget> locationTarget,
                   boolean noDamage, boolean silent) {
    }

    record LocationTarget(double x, double y, double z) {
    }
}
