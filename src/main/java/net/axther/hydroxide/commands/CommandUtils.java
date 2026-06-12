package net.axther.hydroxide.commands;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class CommandUtils {

    private CommandUtils() {
    }

    public static Optional<Player> onlinePlayer(String name) {
        Player exact = Bukkit.getPlayerExact(name);
        if (exact != null) {
            return Optional.of(exact);
        }
        String lowered = name.toLowerCase(Locale.ROOT);
        return Bukkit.getOnlinePlayers().stream()
                .filter(player -> player.getName().toLowerCase(Locale.ROOT).startsWith(lowered))
                .map(Player.class::cast)
                .findFirst();
    }

    public static Optional<Player> playerSender(CommandSender sender) {
        return sender instanceof Player player ? Optional.of(player) : Optional.empty();
    }

    public static String joinArgs(String[] args, int startIndex) {
        if (startIndex >= args.length) {
            return "";
        }
        return String.join(" ", Arrays.copyOfRange(args, startIndex, args.length));
    }

    public static List<String> matching(String prefix, Iterable<String> values) {
        return CompletionUtils.matching(prefix, values);
    }

    public static Optional<GameMode> gameMode(String input) {
        String normalized = input.toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "0", "S", "SURVIVAL" -> Optional.of(GameMode.SURVIVAL);
            case "1", "C", "CREATIVE" -> Optional.of(GameMode.CREATIVE);
            case "2", "A", "ADVENTURE" -> Optional.of(GameMode.ADVENTURE);
            case "3", "SP", "SPECTATOR" -> Optional.of(GameMode.SPECTATOR);
            default -> Optional.empty();
        };
    }
}
