package net.axther.hydroxide.modules.teleport;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

final class TpAllWorldCommandParser {

    private TpAllWorldCommandParser() {
    }

    static Optional<Request> parse(List<String> args) {
        List<String> values = new ArrayList<>();
        boolean includeAll = false;
        for (String arg : args) {
            if (arg.equalsIgnoreCase("-a")) {
                includeAll = true;
            } else {
                values.add(arg);
            }
        }
        if (values.isEmpty() || values.size() > 2) {
            return Optional.empty();
        }
        Optional<Destination> destination = values.size() == 2 ? destination(values.get(1)) : Optional.empty();
        if (values.size() == 2 && destination.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new Request(values.getFirst(), destination, includeAll));
    }

    private static Optional<Destination> destination(String input) {
        String[] parts = input.split(";");
        if (parts.length != 4 && parts.length != 6) {
            return Optional.empty();
        }
        try {
            double x = Double.parseDouble(parts[1]);
            double y = Double.parseDouble(parts[2]);
            double z = Double.parseDouble(parts[3]);
            float yaw = parts.length == 6 ? Float.parseFloat(parts[4]) : 0.0F;
            float pitch = parts.length == 6 ? Float.parseFloat(parts[5]) : 0.0F;
            if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)
                    || !Float.isFinite(yaw) || !Float.isFinite(pitch)) {
                return Optional.empty();
            }
            return Optional.of(new Destination(parts[0], x, y, z, yaw, pitch));
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }
    }

    record Request(String sourceWorld, Optional<Destination> destination, boolean includeAll) {
    }

    record Destination(String worldName, double x, double y, double z, float yaw, float pitch) {
    }
}
