package net.axther.hydroxide.modules.chat;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

final class ColorPalette {

    private static final Pattern HEX = Pattern.compile("#?[A-Fa-f0-9]{6}");
    private static final List<Entry> ENTRIES = List.of(
            new Entry("black", "&0", "#000000"),
            new Entry("dark_blue", "&1", "#0000AA"),
            new Entry("dark_green", "&2", "#00AA00"),
            new Entry("dark_aqua", "&3", "#00AAAA"),
            new Entry("dark_red", "&4", "#AA0000"),
            new Entry("dark_purple", "&5", "#AA00AA"),
            new Entry("gold", "&6", "#FFAA00"),
            new Entry("gray", "&7", "#AAAAAA"),
            new Entry("dark_gray", "&8", "#555555"),
            new Entry("blue", "&9", "#5555FF"),
            new Entry("green", "&a", "#55FF55"),
            new Entry("aqua", "&b", "#55FFFF"),
            new Entry("red", "&c", "#FF5555"),
            new Entry("light_purple", "&d", "#FF55FF"),
            new Entry("yellow", "&e", "#FFFF55"),
            new Entry("white", "&f", "#FFFFFF")
    );

    private ColorPalette() {
    }

    static List<Entry> entries() {
        return ENTRIES;
    }

    static Optional<Selection> pick(String input) {
        if (input == null || input.isBlank()) {
            return Optional.empty();
        }
        String value = input.trim();
        Optional<Entry> named = ENTRIES.stream()
                .filter(entry -> entry.name().equalsIgnoreCase(value)
                        || entry.legacy().equalsIgnoreCase(value)
                        || entry.legacy().replace("&", "\u00A7").equalsIgnoreCase(value))
                .findFirst();
        if (named.isPresent()) {
            Entry entry = named.orElseThrow();
            return Optional.of(new Selection(entry.hex(), entry));
        }
        if (!HEX.matcher(value).matches()) {
            return Optional.empty();
        }
        String hex = value.startsWith("#") ? value.toUpperCase(Locale.ROOT) : "#" + value.toUpperCase(Locale.ROOT);
        return Optional.of(new Selection(hex, closest(hex)));
    }

    private static Entry closest(String hex) {
        int red = Integer.parseInt(hex.substring(1, 3), 16);
        int green = Integer.parseInt(hex.substring(3, 5), 16);
        int blue = Integer.parseInt(hex.substring(5, 7), 16);
        return ENTRIES.stream()
                .min(Comparator.comparingInt(entry -> distance(red, green, blue, entry)))
                .orElseThrow();
    }

    private static int distance(int red, int green, int blue, Entry entry) {
        int entryRed = Integer.parseInt(entry.hex().substring(1, 3), 16);
        int entryGreen = Integer.parseInt(entry.hex().substring(3, 5), 16);
        int entryBlue = Integer.parseInt(entry.hex().substring(5, 7), 16);
        return square(red - entryRed) + square(green - entryGreen) + square(blue - entryBlue);
    }

    private static int square(int value) {
        return value * value;
    }

    record Entry(String name, String legacy, String hex) {
        String miniMessage() {
            return "<" + hex + ">";
        }

        String literalMiniMessage() {
            return "\\<" + hex + ">";
        }
    }

    record Selection(String hex, Entry closest) {
        String miniMessage() {
            return "<" + hex + ">";
        }

        String literalMiniMessage() {
            return "\\<" + hex + ">";
        }
    }
}
