package net.axther.hydroxide.modules.hologram;

import java.util.Locale;
import java.util.Optional;

public enum HologramDisplayType {
    TEXT,
    ITEM,
    BLOCK;

    public String key() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static Optional<HologramDisplayType> from(String input) {
        for (HologramDisplayType type : values()) {
            if (type.key().equalsIgnoreCase(input)) {
                return Optional.of(type);
            }
        }
        return Optional.empty();
    }
}
