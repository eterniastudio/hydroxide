package net.axther.hydroxide.modules.utility;

import java.util.Locale;
import java.util.Optional;

public record AttachedCommand(Click click, Executor executor, int usesRemaining, String command) {

    public enum Click {
        LEFT,
        RIGHT
    }

    public enum Executor {
        PLAYER,
        CONSOLE
    }

    public static Optional<AttachedCommand> parse(String input) {
        String[] parts = input == null ? new String[0] : input.trim().split("\\s+", 4);
        if (parts.length < 4) {
            return Optional.empty();
        }
        try {
            Click click = Click.valueOf(parts[0].toUpperCase(Locale.ROOT));
            Executor executor = Executor.valueOf(parts[1].toUpperCase(Locale.ROOT));
            int uses = parts[2].equalsIgnoreCase("infinite") ? -1 : Integer.parseInt(parts[2]);
            if (uses == 0 || parts[3].isBlank()) {
                return Optional.empty();
            }
            return Optional.of(new AttachedCommand(click, executor, uses, parts[3]));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    public boolean canAttach(boolean hasConsolePermission) {
        return executor != Executor.CONSOLE || hasConsolePermission;
    }

    public Optional<AttachedCommand> consumeUse() {
        if (usesRemaining < 0) {
            return Optional.of(this);
        }
        if (usesRemaining <= 0) {
            return Optional.empty();
        }
        return Optional.of(new AttachedCommand(click, executor, usesRemaining - 1, command));
    }

    public String render(String player, String uuid, String world, int x, int y, int z) {
        return command
                .replace("{player}", player)
                .replace("{uuid}", uuid)
                .replace("{world}", world)
                .replace("{x}", String.valueOf(x))
                .replace("{y}", String.valueOf(y))
                .replace("{z}", String.valueOf(z));
    }

    public String serialize() {
        return click.name().toLowerCase(Locale.ROOT) + " "
                + executor.name().toLowerCase(Locale.ROOT) + " "
                + (usesRemaining < 0 ? "infinite" : usesRemaining) + " "
                + command;
    }
}
