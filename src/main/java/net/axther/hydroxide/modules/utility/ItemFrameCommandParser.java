package net.axther.hydroxide.modules.utility;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

final class ItemFrameCommandParser {

    private ItemFrameCommandParser() {
    }

    static Optional<Request> parse(List<String> args) {
        if (args.size() != 1) {
            return Optional.empty();
        }
        String input = args.getFirst().toLowerCase(Locale.ROOT);
        return switch (input) {
            case "invisible", "visible", "visibility" -> Optional.of(new Request(Property.INVISIBLE));
            case "fixed", "fix" -> Optional.of(new Request(Property.FIXED));
            case "invulnerable", "invul", "invincible" -> Optional.of(new Request(Property.INVULNERABLE));
            case "all" -> Optional.of(new Request(Property.ALL));
            default -> Optional.empty();
        };
    }

    enum Property {
        INVISIBLE,
        FIXED,
        INVULNERABLE,
        ALL
    }

    record Request(Property property) {
    }
}
