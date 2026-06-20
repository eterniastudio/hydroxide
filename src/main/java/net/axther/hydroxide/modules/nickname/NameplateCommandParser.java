package net.axther.hydroxide.modules.nickname;

import net.kyori.adventure.text.format.NamedTextColor;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

final class NameplateCommandParser {

    private static final Map<String, NamedTextColor> LEGACY_COLORS = Map.ofEntries(
            Map.entry("0", NamedTextColor.BLACK),
            Map.entry("1", NamedTextColor.DARK_BLUE),
            Map.entry("2", NamedTextColor.DARK_GREEN),
            Map.entry("3", NamedTextColor.DARK_AQUA),
            Map.entry("4", NamedTextColor.DARK_RED),
            Map.entry("5", NamedTextColor.DARK_PURPLE),
            Map.entry("6", NamedTextColor.GOLD),
            Map.entry("7", NamedTextColor.GRAY),
            Map.entry("8", NamedTextColor.DARK_GRAY),
            Map.entry("9", NamedTextColor.BLUE),
            Map.entry("a", NamedTextColor.GREEN),
            Map.entry("b", NamedTextColor.AQUA),
            Map.entry("c", NamedTextColor.RED),
            Map.entry("d", NamedTextColor.LIGHT_PURPLE),
            Map.entry("e", NamedTextColor.YELLOW),
            Map.entry("f", NamedTextColor.WHITE)
    );

    private NameplateCommandParser() {
    }

    static Optional<Request> parse(List<String> args) {
        if (args.isEmpty()) {
            return Optional.empty();
        }

        int index = 0;
        Optional<String> targetName = Optional.empty();
        String first = args.getFirst();
        if (!isFlag(first) && !first.equalsIgnoreCase("reset")) {
            targetName = Optional.of(first);
            index++;
        }

        boolean reset = false;
        boolean silent = false;
        Optional<String> prefix = Optional.empty();
        Optional<String> suffix = Optional.empty();
        Optional<NamedTextColor> color = Optional.empty();
        boolean colorProvided = false;

        for (; index < args.size(); index++) {
            String token = args.get(index);
            if (token.equalsIgnoreCase("-s")) {
                silent = true;
                continue;
            }
            if (token.equalsIgnoreCase("reset")) {
                reset = true;
                continue;
            }
            if (startsWithAny(token, "-pref:", "pref:", "-prefix:", "prefix:")) {
                prefix = Optional.of(valueAfterColon(token));
                continue;
            }
            if (startsWithAny(token, "-suf:", "suf:", "-suffix:", "suffix:")) {
                suffix = Optional.of(valueAfterColon(token));
                continue;
            }
            if (startsWithAny(token, "-c:", "c:", "-color:", "color:")) {
                String rawColor = valueAfterColon(token);
                colorProvided = true;
                if (!rawColor.equalsIgnoreCase("reset") && !rawColor.equalsIgnoreCase("clear")) {
                    Optional<NamedTextColor> parsedColor = parseColor(rawColor);
                    if (parsedColor.isEmpty()) {
                        return Optional.empty();
                    }
                    color = parsedColor;
                }
                continue;
            }
            return Optional.empty();
        }

        boolean hasUpdate = prefix.isPresent() || suffix.isPresent() || colorProvided;
        if (reset && hasUpdate) {
            return Optional.empty();
        }
        if (!reset && !hasUpdate) {
            return Optional.empty();
        }
        return Optional.of(new Request(targetName, prefix, suffix, color, colorProvided, reset, silent));
    }

    static Optional<NamedTextColor> parseColor(String input) {
        String normalized = input.trim()
                .toLowerCase(Locale.ROOT)
                .replace("§", "&")
                .replace("-", "_");
        if (normalized.startsWith("<") && normalized.endsWith(">")) {
            normalized = normalized.substring(1, normalized.length() - 1);
        }
        if (normalized.startsWith("&") && normalized.length() == 2) {
            normalized = normalized.substring(1);
        }
        if (normalized.equals("grey")) {
            normalized = "gray";
        }
        if (normalized.equals("dark_grey")) {
            normalized = "dark_gray";
        }
        NamedTextColor legacy = LEGACY_COLORS.get(normalized);
        if (legacy != null) {
            return Optional.of(legacy);
        }
        return Optional.ofNullable(NamedTextColor.NAMES.value(normalized));
    }

    static String colorName(NamedTextColor color) {
        return NamedTextColor.NAMES.keyOr(color, "white");
    }

    private static boolean isFlag(String token) {
        return token.startsWith("-") || token.contains(":");
    }

    private static boolean startsWithAny(String value, String... prefixes) {
        String lowered = value.toLowerCase(Locale.ROOT);
        for (String prefix : prefixes) {
            if (lowered.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private static String valueAfterColon(String value) {
        int split = value.indexOf(':');
        return split == -1 ? "" : value.substring(split + 1);
    }

    record Request(
            Optional<String> targetName,
            Optional<String> prefix,
            Optional<String> suffix,
            Optional<NamedTextColor> color,
            boolean colorProvided,
            boolean reset,
            boolean silent
    ) {
    }
}
