package net.axther.hydroxide.modules.utility;

import org.bukkit.Color;
import org.bukkit.DyeColor;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class DyeCommandParser {

    private static final Pattern HEX = Pattern.compile("#?([0-9a-fA-F]{6})");
    private static final Pattern RGB = Pattern.compile("(\\d{1,3}),(\\d{1,3}),(\\d{1,3})");

    private DyeCommandParser() {
    }

    static Optional<Request> parse(List<String> args) {
        if (args.size() != 1) {
            return Optional.empty();
        }

        String input = args.getFirst().trim().toLowerCase(Locale.ROOT);
        if (input.isBlank()) {
            return Optional.empty();
        }
        if (isClear(input)) {
            return Optional.of(new Request(Action.CLEAR, Optional.empty()));
        }
        if (input.equals("random")) {
            return Optional.of(new Request(Action.RANDOM, Optional.empty()));
        }

        Optional<Color> hex = parseHex(input);
        if (hex.isPresent()) {
            return Optional.of(new Request(Action.SET, hex));
        }

        Optional<Color> rgb = parseRgb(input);
        if (rgb.isPresent()) {
            return Optional.of(new Request(Action.SET, rgb));
        }

        try {
            DyeColor dyeColor = DyeColor.valueOf(input.replace('-', '_').toUpperCase(Locale.ROOT));
            return Optional.of(new Request(Action.SET, Optional.of(dyeColor.getColor())));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    private static Optional<Color> parseHex(String input) {
        Matcher matcher = HEX.matcher(input);
        if (!matcher.matches()) {
            return Optional.empty();
        }
        int value = Integer.parseInt(matcher.group(1), 16);
        return Optional.of(Color.fromRGB((value >> 16) & 0xFF, (value >> 8) & 0xFF, value & 0xFF));
    }

    private static Optional<Color> parseRgb(String input) {
        Matcher matcher = RGB.matcher(input);
        if (!matcher.matches()) {
            return Optional.empty();
        }
        int red = Integer.parseInt(matcher.group(1));
        int green = Integer.parseInt(matcher.group(2));
        int blue = Integer.parseInt(matcher.group(3));
        if (!isRgb(red) || !isRgb(green) || !isRgb(blue)) {
            return Optional.empty();
        }
        return Optional.of(Color.fromRGB(red, green, blue));
    }

    private static boolean isRgb(int value) {
        return value >= 0 && value <= 255;
    }

    private static boolean isClear(String input) {
        return switch (input) {
            case "clear", "reset", "none" -> true;
            default -> false;
        };
    }

    enum Action {
        SET,
        RANDOM,
        CLEAR
    }

    record Request(Action action, Optional<Color> color) {
    }
}
