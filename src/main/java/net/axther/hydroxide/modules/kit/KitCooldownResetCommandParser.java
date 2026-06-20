package net.axther.hydroxide.modules.kit;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

final class KitCooldownResetCommandParser {

    private KitCooldownResetCommandParser() {
    }

    static Optional<Request> parse(List<String> args) {
        if (args.size() != 2 || args.getFirst().isBlank() || args.get(1).isBlank()) {
            return Optional.empty();
        }
        return Optional.of(new Request(args.getFirst().toLowerCase(Locale.ROOT), Target.from(args.get(1))));
    }

    record Target(String name, boolean all) {
        static Target from(String input) {
            return switch (input.toLowerCase(Locale.ROOT)) {
                case "all", "*", "@a" -> new Target("all", true);
                default -> new Target(input, false);
            };
        }
    }

    record Request(String kit, Target target) {
    }
}
