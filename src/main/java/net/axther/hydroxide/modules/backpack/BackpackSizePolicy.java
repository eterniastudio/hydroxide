package net.axther.hydroxide.modules.backpack;

import java.util.Locale;
import java.util.Set;

public final class BackpackSizePolicy {

    private static final String PREFIX = "hydroxide.backpack.size.";
    private final int defaultSlots;
    private final int maxSlots;

    public BackpackSizePolicy(int defaultSlots, int maxSlots) {
        this.defaultSlots = normalize(defaultSlots);
        this.maxSlots = normalize(maxSlots);
    }

    public int slotsFor(Set<String> permissions) {
        int slots = defaultSlots;
        for (String permission : permissions) {
            String normalized = permission.toLowerCase(Locale.ROOT);
            if (!normalized.startsWith(PREFIX)) {
                continue;
            }
            try {
                slots = Math.max(slots, normalize(Integer.parseInt(normalized.substring(PREFIX.length()))));
            } catch (NumberFormatException ignored) {
                // Ignore malformed permission suffixes.
            }
        }
        return Math.min(slots, maxSlots);
    }

    private int normalize(int slots) {
        int clamped = Math.max(9, Math.min(54, slots));
        return (clamped / 9) * 9;
    }
}
