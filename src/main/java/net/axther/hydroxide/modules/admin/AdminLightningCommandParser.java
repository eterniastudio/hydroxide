package net.axther.hydroxide.modules.admin;

import java.util.List;
import java.util.Optional;

final class AdminLightningCommandParser {

    private AdminLightningCommandParser() {
    }

    static Optional<Request> parse(List<String> args) {
        if (args.isEmpty()) {
            return Optional.empty();
        }

        TargetType targetType = null;
        Optional<String> targetName = Optional.empty();
        Optional<Coordinates> coordinates = Optional.empty();
        boolean safe = false;
        boolean silent = false;

        for (String arg : args) {
            if (arg.equalsIgnoreCase("-safe")) {
                safe = true;
            } else if (arg.equalsIgnoreCase("-s")) {
                silent = true;
            } else if (arg.contains(";")) {
                if (targetType != null) {
                    return Optional.empty();
                }
                Optional<Coordinates> parsed = coordinates(arg);
                if (parsed.isEmpty()) {
                    return Optional.empty();
                }
                targetType = TargetType.COORDINATES;
                coordinates = parsed;
            } else if (!arg.startsWith("-")) {
                if (targetType != null || arg.isBlank()) {
                    return Optional.empty();
                }
                targetType = TargetType.PLAYER;
                targetName = Optional.of(arg);
            } else {
                return Optional.empty();
            }
        }

        if (targetType == null) {
            return Optional.empty();
        }
        return Optional.of(new Request(targetType, targetName, coordinates, safe, silent));
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
        PLAYER,
        COORDINATES
    }

    record Coordinates(String worldName, double x, double y, double z) {
    }

    record Request(TargetType targetType, Optional<String> targetName,
                   Optional<Coordinates> coordinates, boolean safe, boolean silent) {
    }
}
