package net.axther.hydroxide.modules.bridge;

import java.util.LinkedHashMap;
import java.util.Map;

public final class BridgeMessageCodec {

    private BridgeMessageCodec() {
    }

    public static String encode(BridgeMessage message) {
        return "{"
                + "\"serverId\":\"" + escape(message.serverId()) + "\","
                + "\"channel\":\"" + escape(message.channel()) + "\","
                + "\"sender\":\"" + escape(message.sender()) + "\","
                + "\"message\":\"" + escape(message.message()) + "\""
                + "}";
    }

    public static BridgeMessage decode(String json) {
        Map<String, String> values = parseFlatJson(json);
        return new BridgeMessage(
                values.getOrDefault("serverId", ""),
                values.getOrDefault("channel", ""),
                values.getOrDefault("sender", ""),
                values.getOrDefault("message", "")
        );
    }

    private static String escape(String value) {
        return (value == null ? "" : value)
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }

    private static Map<String, String> parseFlatJson(String json) {
        Map<String, String> values = new LinkedHashMap<>();
        String trimmed = json == null ? "" : json.trim();
        if (trimmed.startsWith("{")) {
            trimmed = trimmed.substring(1);
        }
        if (trimmed.endsWith("}")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }

        int index = 0;
        while (index < trimmed.length()) {
            int keyStart = trimmed.indexOf('"', index);
            if (keyStart < 0) {
                break;
            }
            int keyEnd = readStringEnd(trimmed, keyStart + 1);
            int valueStart = trimmed.indexOf('"', trimmed.indexOf(':', keyEnd) + 1);
            int valueEnd = readStringEnd(trimmed, valueStart + 1);
            values.put(unescape(trimmed.substring(keyStart + 1, keyEnd)), unescape(trimmed.substring(valueStart + 1, valueEnd)));
            index = valueEnd + 1;
        }
        return values;
    }

    private static int readStringEnd(String input, int start) {
        boolean escaped = false;
        for (int index = start; index < input.length(); index++) {
            char current = input.charAt(index);
            if (escaped) {
                escaped = false;
                continue;
            }
            if (current == '\\') {
                escaped = true;
                continue;
            }
            if (current == '"') {
                return index;
            }
        }
        return input.length();
    }

    private static String unescape(String value) {
        StringBuilder builder = new StringBuilder(value.length());
        boolean escaped = false;
        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);
            if (escaped) {
                builder.append(current);
                escaped = false;
            } else if (current == '\\') {
                escaped = true;
            } else {
                builder.append(current);
            }
        }
        return builder.toString();
    }
}
