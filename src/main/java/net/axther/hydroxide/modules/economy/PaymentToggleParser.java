package net.axther.hydroxide.modules.economy;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

final class PaymentToggleParser {

    private PaymentToggleParser() {
    }

    static Optional<Boolean> parse(List<String> args, boolean currentState) {
        if (args.isEmpty()) {
            return Optional.of(!currentState);
        }
        if (args.size() > 1) {
            return Optional.empty();
        }
        return switch (args.getFirst().toLowerCase(Locale.ROOT)) {
            case "on", "true", "yes", "enable", "enabled" -> Optional.of(true);
            case "off", "false", "no", "disable", "disabled" -> Optional.of(false);
            default -> Optional.empty();
        };
    }
}
