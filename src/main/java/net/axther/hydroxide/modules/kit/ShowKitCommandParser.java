package net.axther.hydroxide.modules.kit;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

final class ShowKitCommandParser {

    private ShowKitCommandParser() {
    }

    static Optional<Request> parse(List<String> args) {
        if (args.isEmpty() || args.size() > 2) {
            return Optional.empty();
        }
        String kit = args.getFirst().trim();
        if (kit.isBlank() || kit.startsWith("-")) {
            return Optional.empty();
        }
        Optional<String> target = args.size() == 2 && !args.get(1).isBlank()
                ? Optional.of(args.get(1))
                : Optional.empty();
        return Optional.of(new Request(kit.toLowerCase(Locale.ROOT), target));
    }

    record Request(String kit, Optional<String> target) {
    }
}
