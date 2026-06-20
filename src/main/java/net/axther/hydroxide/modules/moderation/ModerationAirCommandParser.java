package net.axther.hydroxide.modules.moderation;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalInt;

final class ModerationAirCommandParser {

    private ModerationAirCommandParser() {
    }

    static Optional<Request> parse(List<String> args) {
        if (args.size() != 2) {
            return Optional.empty();
        }

        Target target = Target.from(args.get(0));
        String amount = args.get(1).toLowerCase(Locale.ROOT);
        if (amount.equals("max") || amount.equals("maximum") || amount.equals("full")) {
            return Optional.of(new Request(target, OptionalInt.empty(), true));
        }
        try {
            int ticks = Integer.parseInt(amount);
            return ticks < 0 ? Optional.empty() : Optional.of(new Request(target, OptionalInt.of(Math.min(ticks, 30_000)), false));
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }
    }

    record Target(String name, boolean all) {
        static Target from(String input) {
            return switch (input.toLowerCase(Locale.ROOT)) {
                case "all", "*", "@a" -> new Target("all", true);
                default -> new Target(input, false);
            };
        }
    }

    record Request(Target target, OptionalInt amountTicks, boolean useMaximum) {
    }
}
