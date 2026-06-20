package net.axther.hydroxide.modules.chat;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

final class BroadcastCommandParser {

    private BroadcastCommandParser() {
    }

    static Optional<Request> parse(List<String> args) {
        if (args.isEmpty()) {
            return Optional.empty();
        }

        boolean clean = false;
        List<String> message = new ArrayList<>();
        List<String> worldNames = List.of();
        Optional<Double> radius = Optional.empty();
        Optional<Coordinates> center = Optional.empty();

        for (String arg : args) {
            if (arg.equals("!")) {
                clean = true;
            } else if (arg.regionMatches(true, 0, "-w:", 0, 3)) {
                Optional<List<String>> parsed = worlds(arg.substring(3));
                if (parsed.isEmpty()) {
                    return Optional.empty();
                }
                worldNames = parsed.orElseThrow();
            } else if (arg.regionMatches(true, 0, "-r:", 0, 3)) {
                Optional<Double> parsed = positiveDouble(arg.substring(3));
                if (parsed.isEmpty()) {
                    return Optional.empty();
                }
                radius = parsed;
            } else if (arg.regionMatches(true, 0, "-c:", 0, 3)) {
                Optional<Coordinates> parsed = coordinates(arg.substring(3));
                if (parsed.isEmpty()) {
                    return Optional.empty();
                }
                center = parsed;
            } else if (arg.startsWith("-")) {
                return Optional.empty();
            } else {
                if (message.isEmpty() && arg.startsWith("!") && arg.length() > 1) {
                    clean = true;
                    message.add(arg.substring(1));
                } else {
                    message.add(arg);
                }
            }
        }

        String renderedMessage = String.join(" ", message).trim();
        if (renderedMessage.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new Request(renderedMessage, clean, List.copyOf(worldNames), radius, center));
    }

    private static Optional<List<String>> worlds(String value) {
        if (value.isBlank()) {
            return Optional.empty();
        }
        List<String> worlds = new ArrayList<>();
        for (String part : value.split(",")) {
            String world = part.trim();
            if (world.isBlank()) {
                return Optional.empty();
            }
            worlds.add(world);
        }
        return Optional.of(worlds);
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

    record Coordinates(String worldName, double x, double y, double z) {
    }

    record Request(String message, boolean clean, List<String> worldNames,
                   Optional<Double> radius, Optional<Coordinates> center) {

        boolean filtered() {
            return !worldNames.isEmpty() || radius.isPresent() || center.isPresent();
        }
    }
}
