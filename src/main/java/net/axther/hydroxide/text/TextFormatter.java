package net.axther.hydroxide.text;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

public final class TextFormatter {

    private static final Pattern HEX = Pattern.compile("[0-9a-fA-F]{6}");
    private static final Map<Character, String> LEGACY_TAGS = Map.ofEntries(
            Map.entry('0', "black"),
            Map.entry('1', "dark_blue"),
            Map.entry('2', "dark_green"),
            Map.entry('3', "dark_aqua"),
            Map.entry('4', "dark_red"),
            Map.entry('5', "dark_purple"),
            Map.entry('6', "gold"),
            Map.entry('7', "gray"),
            Map.entry('8', "dark_gray"),
            Map.entry('9', "blue"),
            Map.entry('a', "green"),
            Map.entry('b', "aqua"),
            Map.entry('c', "red"),
            Map.entry('d', "light_purple"),
            Map.entry('e', "yellow"),
            Map.entry('f', "white"),
            Map.entry('k', "obfuscated"),
            Map.entry('l', "bold"),
            Map.entry('m', "strikethrough"),
            Map.entry('n', "underlined"),
            Map.entry('o', "italic"),
            Map.entry('r', "reset")
    );

    private final MiniMessage miniMessage;
    private final PlainTextComponentSerializer plainText;

    public TextFormatter() {
        this(MiniMessage.miniMessage(), PlainTextComponentSerializer.plainText());
    }

    TextFormatter(MiniMessage miniMessage, PlainTextComponentSerializer plainText) {
        this.miniMessage = miniMessage;
        this.plainText = plainText;
    }

    public Component format(String input) {
        if (input == null || input.isBlank()) {
            return Component.empty();
        }
        return miniMessage.deserialize(normalizeLegacySyntax(input));
    }

    public String plain(Component component) {
        return plainText.serialize(component == null ? Component.empty() : component);
    }

    public String literal(String input) {
        if (input == null || input.isBlank()) {
            return "";
        }
        return input.replace("&", "&&").replace("<", "\\<");
    }

    public String normalizeLegacySyntax(String input) {
        StringBuilder builder = new StringBuilder(input.length());
        for (int index = 0; index < input.length(); index++) {
            char current = input.charAt(index);
            if (current != '&' || index + 1 >= input.length()) {
                builder.append(current);
                continue;
            }

            char next = input.charAt(index + 1);
            if (next == '&') {
                builder.append('&');
                index++;
                continue;
            }

            if (next == '#' && index + 7 < input.length()) {
                String hex = input.substring(index + 2, index + 8);
                if (HEX.matcher(hex).matches()) {
                    builder.append("<#").append(hex.toUpperCase(Locale.ROOT)).append(">");
                    index += 7;
                    continue;
                }
            }

            String expandedHex = readLegacyExpandedHex(input, index);
            if (expandedHex != null) {
                builder.append("<#").append(expandedHex).append(">");
                index += 13;
                continue;
            }

            String tag = LEGACY_TAGS.get(Character.toLowerCase(next));
            if (tag != null) {
                builder.append('<').append(tag).append('>');
                index++;
                continue;
            }

            builder.append(current);
        }
        return builder.toString();
    }

    private String readLegacyExpandedHex(String input, int ampersandIndex) {
        if (ampersandIndex + 13 >= input.length()
                || Character.toLowerCase(input.charAt(ampersandIndex + 1)) != 'x') {
            return null;
        }

        StringBuilder hex = new StringBuilder(6);
        for (int offset = ampersandIndex + 2; offset <= ampersandIndex + 12; offset += 2) {
            if (input.charAt(offset) != '&') {
                return null;
            }
            char digit = input.charAt(offset + 1);
            if (!HEX.matcher(String.valueOf(digit).repeat(6)).matches()) {
                return null;
            }
            hex.append(Character.toUpperCase(digit));
        }
        return hex.toString();
    }
}
