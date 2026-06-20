package net.axther.hydroxide.modules.admin;

import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;

final class AdminSoundCommandParser {

    private AdminSoundCommandParser() {
    }

    static Optional<Request> parse(List<String> args) {
        if (args.isEmpty() || args.getFirst().isBlank() || args.getFirst().startsWith("-")) {
            return Optional.empty();
        }

        String soundName = args.getFirst();
        TargetType targetType = TargetType.SELF;
        Optional<String> targetName = Optional.empty();
        Optional<Coordinates> coordinates = Optional.empty();
        OptionalDouble radius = OptionalDouble.empty();
        float volume = 1.0F;
        float pitch = 1.0F;
        boolean silent = false;

        for (int index = 1; index < args.size(); index++) {
            String arg = args.get(index);
            if (arg.equalsIgnoreCase("-s")) {
                silent = true;
            } else if (arg.equalsIgnoreCase("-all")) {
                if (targetType != TargetType.SELF) {
                    return Optional.empty();
                }
                targetType = TargetType.ALL;
            } else if (arg.regionMatches(true, 0, "-p:", 0, 3)) {
                var parsed = positiveFloat(arg.substring(3));
                if (parsed.isEmpty()) {
                    return Optional.empty();
                }
                pitch = parsed.get();
            } else if (arg.regionMatches(true, 0, "-v:", 0, 3)) {
                var parsed = positiveFloat(arg.substring(3));
                if (parsed.isEmpty()) {
                    return Optional.empty();
                }
                volume = parsed.get();
            } else if (arg.regionMatches(true, 0, "-r:", 0, 3)) {
                var parsed = positiveDouble(arg.substring(3));
                if (parsed.isEmpty()) {
                    return Optional.empty();
                }
                radius = OptionalDouble.of(parsed.get());
            } else if (arg.regionMatches(true, 0, "-l:", 0, 3)) {
                if (targetType != TargetType.SELF || arg.length() <= 3) {
                    return Optional.empty();
                }
                targetType = TargetType.PLAYER_LOCATION;
                targetName = Optional.of(arg.substring(3));
            } else if (arg.contains(";")) {
                if (targetType != TargetType.SELF) {
                    return Optional.empty();
                }
                var parsed = coordinates(arg);
                if (parsed.isEmpty()) {
                    return Optional.empty();
                }
                targetType = TargetType.COORDINATES;
                coordinates = parsed;
            } else if (!arg.startsWith("-")) {
                if (targetType != TargetType.SELF) {
                    return Optional.empty();
                }
                targetType = TargetType.PLAYER;
                targetName = Optional.of(arg);
            } else {
                return Optional.empty();
            }
        }

        return Optional.of(new Request(soundName, targetType, targetName, coordinates, radius, volume, pitch, silent));
    }

    private static Optional<Float> positiveFloat(String value) {
        try {
            float parsed = Float.parseFloat(value);
            return Float.isFinite(parsed) && parsed > 0.0F ? Optional.of(parsed) : Optional.empty();
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }
    }

    private static Optional<Double> positiveDouble(String value) {
        try {
            double parsed = Double.parseDouble(value);
            return Double.isFinite(parsed) && parsed > 0.0D ? Optional.of(parsed) : Optional.empty();
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }
    }

    private static Optional<Coordinates> coordinates(String value) {
        String[] parts = value.split(";");
        if (parts.length != 4 || parts[0].isBlank()) {
            return Optional.empty();
        }
        try {
            double x = Double.parseDouble(parts[1]);
            double y = Double.parseDouble(parts[2]);
            double z = Double.parseDouble(parts[3]);
            if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) {
                return Optional.empty();
            }
            return Optional.of(new Coordinates(parts[0], x, y, z));
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }
    }

    enum TargetType {
        SELF,
        PLAYER,
        ALL,
        PLAYER_LOCATION,
        COORDINATES
    }

    record Coordinates(String worldName, double x, double y, double z) {
    }

    record Request(String soundName, TargetType targetType, Optional<String> targetName,
                   Optional<Coordinates> coordinates, OptionalDouble radius, float volume,
                   float pitch, boolean silent) {
    }
}
