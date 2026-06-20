package net.axther.hydroxide.modules.kit;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

final class KitCommandParser {

    private KitCommandParser() {
    }

    static Optional<Request> parse(List<String> args) {
        if (args.isEmpty()) {
            return Optional.of(new Request(Mode.MENU, Optional.empty(), Optional.empty(), false, false));
        }
        String kit = args.getFirst().trim();
        if (kit.isBlank() || kit.startsWith("-")) {
            return Optional.empty();
        }

        Mode mode = Mode.CLAIM;
        Optional<String> target = Optional.empty();
        boolean silent = false;
        boolean ignoreCooldown = false;
        for (String raw : args.subList(1, args.size())) {
            String token = raw.trim();
            if (token.isBlank()) {
                return Optional.empty();
            }
            String normalized = token.toLowerCase(Locale.ROOT);
            switch (normalized) {
                case "-s" -> silent = true;
                case "-c" -> ignoreCooldown = true;
                case "-preview" -> {
                    if (mode != Mode.CLAIM) {
                        return Optional.empty();
                    }
                    mode = Mode.PREVIEW;
                }
                case "-open" -> {
                    if (mode != Mode.CLAIM) {
                        return Optional.empty();
                    }
                    mode = Mode.OPEN;
                }
                default -> {
                    if (normalized.startsWith("-") || target.isPresent()) {
                        return Optional.empty();
                    }
                    target = Optional.of(token);
                }
            }
        }
        return Optional.of(new Request(mode, Optional.of(kit.toLowerCase(Locale.ROOT)), target, silent, ignoreCooldown));
    }

    enum Mode {
        MENU,
        CLAIM,
        PREVIEW,
        OPEN
    }

    record Request(Mode mode, Optional<String> kit, Optional<String> target, boolean silent, boolean ignoreCooldown) {
    }
}
