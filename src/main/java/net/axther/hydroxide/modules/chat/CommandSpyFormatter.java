package net.axther.hydroxide.modules.chat;

import java.util.List;
import java.util.Locale;

final class CommandSpyFormatter {

    private CommandSpyFormatter() {
    }

    static String displayCommand(String command) {
        String trimmed = command.trim();
        return trimmed.startsWith("/") ? trimmed.substring(1) : trimmed;
    }

    static boolean shouldSkip(String command, List<String> excludedPrefixes) {
        String display = displayCommand(command).toLowerCase(Locale.ROOT);
        for (String prefix : excludedPrefixes) {
            String normalized = prefix.trim().toLowerCase(Locale.ROOT);
            if (normalized.startsWith("/")) {
                normalized = normalized.substring(1);
            }
            if (normalized.isBlank()) {
                continue;
            }
            if (display.equals(normalized) || display.startsWith(normalized + " ")) {
                return true;
            }
        }
        return false;
    }

    static List<String> defaultExcludedPrefixes() {
        return List.of("login", "register", "changepassword");
    }
}
