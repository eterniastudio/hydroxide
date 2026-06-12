package net.axther.hydroxide.commands;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;
import java.util.stream.IntStream;

public final class CompletionUtils {

    private CompletionUtils() {
    }

    public static List<String> matching(String prefix, Iterable<String> values) {
        String lowered = prefix.toLowerCase(Locale.ROOT);
        return java.util.stream.StreamSupport.stream(values.spliterator(), false)
                .filter(value -> value.toLowerCase(Locale.ROOT).startsWith(lowered))
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    public static List<String> onlinePlayers(String prefix) {
        return matching(prefix, Bukkit.getOnlinePlayers().stream().map(Player::getName).toList());
    }

    public static List<String> worlds(String prefix) {
        return matching(prefix, Bukkit.getWorlds().stream().map(World::getName).toList());
    }

    public static List<String> materials(String prefix) {
        return matching(prefix, java.util.Arrays.stream(Material.values())
                .filter(Material::isItem)
                .map(material -> material.name().toLowerCase(Locale.ROOT))
                .toList());
    }

    public static List<String> integerRange(String prefix, int min, int max) {
        return IntStream.rangeClosed(min, max)
                .mapToObj(String::valueOf)
                .filter(value -> value.startsWith(prefix))
                .toList();
    }
}
