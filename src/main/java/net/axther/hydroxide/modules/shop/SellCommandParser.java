package net.axther.hydroxide.modules.shop;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalInt;

final class SellCommandParser {

    private SellCommandParser() {
    }

    static Optional<SellCommandRequest> parse(String label, List<String> args) {
        if (args.isEmpty()) {
            SellCommandRequest.Mode mode = label.equalsIgnoreCase("sellall")
                    ? SellCommandRequest.Mode.ALL
                    : SellCommandRequest.Mode.HAND;
            return Optional.of(new SellCommandRequest(mode, Optional.empty(), OptionalInt.empty()));
        }
        if (args.size() > 2) {
            return Optional.empty();
        }

        String first = args.getFirst().toLowerCase(Locale.ROOT);
        return switch (first) {
            case "all", "inventory" -> args.size() == 1
                    ? Optional.of(new SellCommandRequest(SellCommandRequest.Mode.ALL, Optional.empty(), OptionalInt.empty()))
                    : Optional.empty();
            case "hand", "held" -> amount(args, 1)
                    .map(value -> new SellCommandRequest(SellCommandRequest.Mode.HAND, Optional.empty(), value));
            default -> parseDefault(first, args);
        };
    }

    private static Optional<SellCommandRequest> parseDefault(String first, List<String> args) {
        Optional<Integer> handAmount = amount(first);
        if (args.size() == 1 && handAmount.isPresent()) {
            return Optional.of(new SellCommandRequest(SellCommandRequest.Mode.HAND, Optional.empty(), OptionalInt.of(handAmount.get())));
        }
        return amount(args, 1)
                .map(value -> new SellCommandRequest(SellCommandRequest.Mode.MATERIAL, Optional.of(first), value));
    }

    private static Optional<OptionalInt> amount(List<String> args, int index) {
        if (args.size() <= index) {
            return Optional.of(OptionalInt.empty());
        }
        return amount(args.get(index)).map(OptionalInt::of);
    }

    private static Optional<Integer> amount(String input) {
        try {
            int parsed = Integer.parseInt(input);
            return parsed > 0 ? Optional.of(parsed) : Optional.empty();
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }
    }
}
