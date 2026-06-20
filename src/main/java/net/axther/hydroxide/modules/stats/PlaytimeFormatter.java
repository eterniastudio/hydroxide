package net.axther.hydroxide.modules.stats;

import java.util.ArrayList;
import java.util.List;

final class PlaytimeFormatter {

    private PlaytimeFormatter() {
    }

    static String format(long seconds) {
        long remaining = Math.max(0L, seconds);
        long days = remaining / 86_400L;
        remaining %= 86_400L;
        long hours = remaining / 3_600L;
        remaining %= 3_600L;
        long minutes = remaining / 60L;
        remaining %= 60L;

        List<String> parts = new ArrayList<>();
        if (days > 0L) {
            parts.add(days + "d");
        }
        if (hours > 0L) {
            parts.add(hours + "h");
        }
        if (minutes > 0L) {
            parts.add(minutes + "m");
        }
        if (remaining > 0L || parts.isEmpty()) {
            parts.add(remaining + "s");
        }
        return String.join(" ", parts);
    }
}
