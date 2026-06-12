package net.axther.hydroxide.registry;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class ModernRegistryKeys {

    private ModernRegistryKeys() {
    }

    public static String minecraftKey(String input) {
        String value = input == null ? "" : input.trim().toLowerCase(Locale.ROOT);
        int namespaceIndex = value.indexOf(':');
        if (namespaceIndex >= 0) {
            value = value.substring(namespaceIndex + 1);
        }
        return value.replace(' ', '_');
    }

    public static List<String> soundKeys(String input) {
        String normalized = minecraftKey(input);
        List<String> keys = new ArrayList<>();
        add(keys, normalized);
        String dotted = normalized.replace('_', '.');
        add(keys, dotted);
        add(keys, dotted.replace("note.block", "note_block"));
        return List.copyOf(keys);
    }

    public static List<String> gameRuleKeys(String input) {
        String raw = input == null ? "" : input.trim();
        int namespaceIndex = raw.indexOf(':');
        if (namespaceIndex >= 0) {
            raw = raw.substring(namespaceIndex + 1);
        }
        String normalized = minecraftKey(raw);
        List<String> keys = new ArrayList<>();
        add(keys, normalized);
        add(keys, camelToSnake(normalized));
        add(keys, camelToSnake(raw));
        return List.copyOf(keys);
    }

    public static Optional<Object> parseGameRuleValue(Object defaultValue, String rawValue) {
        if (defaultValue instanceof Boolean) {
            return Optional.of(Boolean.parseBoolean(rawValue));
        }
        if (defaultValue instanceof Integer) {
            try {
                return Optional.of(Integer.parseInt(rawValue));
            } catch (NumberFormatException exception) {
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    private static void add(List<String> values, String value) {
        if (!value.isBlank() && !values.contains(value)) {
            values.add(value);
        }
    }

    private static String camelToSnake(String input) {
        StringBuilder builder = new StringBuilder(input.length());
        for (int index = 0; index < input.length(); index++) {
            char current = input.charAt(index);
            if (Character.isUpperCase(current) && index > 0) {
                builder.append('_');
            }
            builder.append(Character.toLowerCase(current));
        }
        return builder.toString();
    }
}
