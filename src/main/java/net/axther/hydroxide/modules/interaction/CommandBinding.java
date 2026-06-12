package net.axther.hydroxide.modules.interaction;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

public record CommandBinding(ExecutionMode mode, String command, double cost, int cooldownSeconds) {

    public static Optional<CommandBinding> fromSignLines(List<String> lines) {
        if (lines == null || lines.size() < 2 || !lines.get(0).equalsIgnoreCase("[Command]")) {
            return Optional.empty();
        }
        String commandLine = lines.get(1);
        ExecutionMode mode = ExecutionMode.PLAYER;
        String command = commandLine;
        if (commandLine.toLowerCase(Locale.ROOT).startsWith("console:")) {
            mode = ExecutionMode.CONSOLE;
            command = commandLine.substring("console:".length());
        } else if (commandLine.toLowerCase(Locale.ROOT).startsWith("player:")) {
            command = commandLine.substring("player:".length());
        }

        double cost = 0.0D;
        int cooldown = 0;
        for (int index = 2; index < lines.size(); index++) {
            String line = lines.get(index).toLowerCase(Locale.ROOT);
            if (line.startsWith("cost:")) {
                cost = parseDouble(line.substring("cost:".length()), 0.0D);
            } else if (line.startsWith("cooldown:")) {
                cooldown = Math.max(0, parseInt(line.substring("cooldown:".length()), 0));
            }
        }
        return Optional.of(new CommandBinding(mode, command, cost, cooldown));
    }

    public boolean readyAt(long lastUseMillis, long nowMillis) {
        return nowMillis - lastUseMillis >= cooldownSeconds * 1000L;
    }

    private static double parseDouble(String input, double fallback) {
        try {
            double value = Double.parseDouble(input);
            return value >= 0.0D && !Double.isNaN(value) && !Double.isInfinite(value) ? value : fallback;
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private static int parseInt(String input, int fallback) {
        try {
            return Integer.parseInt(input);
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    public enum ExecutionMode {
        PLAYER,
        CONSOLE
    }
}
