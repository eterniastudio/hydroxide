package net.axther.hydroxide.modules.teleport;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

final class TeleportPositionParser {

    private static final double MIN_Y = -64.0D;
    private static final double MAX_Y = 4096.0D;

    private TeleportPositionParser() {
    }

    static Optional<Coordinates> coordinates(String x, String y, String z, double baseX, double baseY, double baseZ) {
        Optional<Double> parsedX = coordinate(x, baseX);
        Optional<Double> parsedY = coordinate(y, baseY);
        Optional<Double> parsedZ = coordinate(z, baseZ);
        if (parsedX.isEmpty() || parsedY.isEmpty() || parsedZ.isEmpty()) {
            return Optional.empty();
        }
        double yValue = parsedY.get();
        if (yValue < MIN_Y || yValue > MAX_Y) {
            return Optional.empty();
        }
        return Optional.of(new Coordinates(parsedX.get(), yValue, parsedZ.get()));
    }

    static Optional<Request> request(List<String> args) {
        List<String> values = new ArrayList<>();
        Optional<String> targetName = Optional.empty();
        for (String arg : args) {
            if (arg.startsWith("-p:")) {
                String name = arg.substring("-p:".length());
                if (name.isBlank() || targetName.isPresent()) {
                    return Optional.empty();
                }
                targetName = Optional.of(name);
            } else {
                values.add(arg);
            }
        }
        if (values.size() < 3 || values.size() > 6) {
            return Optional.empty();
        }
        Optional<Float> pitch = Optional.empty();
        Optional<Float> yaw = Optional.empty();
        if (values.size() >= 5) {
            pitch = angle(values.get(4));
            if (pitch.isEmpty()) {
                return Optional.empty();
            }
        }
        if (values.size() == 6) {
            yaw = angle(values.get(5));
            if (yaw.isEmpty()) {
                return Optional.empty();
            }
        }
        return Optional.of(new Request(
                targetName,
                values.get(0),
                values.get(1),
                values.get(2),
                values.size() >= 4 ? Optional.of(values.get(3)) : Optional.empty(),
                pitch,
                yaw
        ));
    }

    private static Optional<Double> coordinate(String input, double base) {
        try {
            if (input.equals("~")) {
                return Optional.of(base);
            }
            if (input.startsWith("~")) {
                return Optional.of(base + Double.parseDouble(input.substring(1)));
            }
            return Optional.of(Double.parseDouble(input));
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }
    }

    private static Optional<Float> angle(String input) {
        try {
            float value = Float.parseFloat(input);
            return Float.isFinite(value) ? Optional.of(value) : Optional.empty();
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }
    }

    record Coordinates(double x, double y, double z) {
    }

    record Request(Optional<String> targetName,
                   String x,
                   String y,
                   String z,
                   Optional<String> worldName,
                   Optional<Float> pitch,
                   Optional<Float> yaw) {
    }
}
