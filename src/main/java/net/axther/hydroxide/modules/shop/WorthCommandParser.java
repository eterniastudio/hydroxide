package net.axther.hydroxide.modules.shop;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;

final class WorthCommandParser {

    private WorthCommandParser() {
    }

    static Optional<WorthCommandRequest> parseWorth(List<String> args) {
        if (args.isEmpty()) {
            return Optional.of(new WorthCommandRequest(WorthCommandRequest.Source.HAND, Optional.empty(), OptionalInt.empty()));
        }
        if (args.size() > 2) {
            return Optional.empty();
        }
        String first = args.getFirst().toLowerCase(Locale.ROOT);
        if (first.equals("hand") || first.equals("held")) {
            return amount(args, 1)
                    .map(value -> new WorthCommandRequest(WorthCommandRequest.Source.HAND, Optional.empty(), value));
        }
        return amount(args, 1)
                .map(value -> new WorthCommandRequest(WorthCommandRequest.Source.MATERIAL, Optional.of(first), value));
    }

    static Optional<SetWorthCommandRequest> parseSetWorth(List<String> args) {
        if (args.size() == 1) {
            return price(args.getFirst())
                    .stream()
                    .mapToObj(value -> new SetWorthCommandRequest(WorthCommandRequest.Source.HAND, Optional.empty(), value))
                    .findFirst();
        }
        if (args.size() == 2) {
            String material = args.getFirst().toLowerCase(Locale.ROOT);
            return price(args.get(1))
                    .stream()
                    .mapToObj(value -> new SetWorthCommandRequest(WorthCommandRequest.Source.MATERIAL, Optional.of(material), value))
                    .findFirst();
        }
        return Optional.empty();
    }

    private static Optional<OptionalInt> amount(List<String> args, int index) {
        if (args.size() <= index) {
            return Optional.of(OptionalInt.empty());
        }
        try {
            int parsed = Integer.parseInt(args.get(index));
            return parsed > 0 ? Optional.of(OptionalInt.of(parsed)) : Optional.empty();
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }
    }

    private static OptionalDouble price(String input) {
        try {
            double parsed = Double.parseDouble(input);
            return ShopPricing.validMoney(parsed) ? OptionalDouble.of(parsed) : OptionalDouble.empty();
        } catch (NumberFormatException exception) {
            return OptionalDouble.empty();
        }
    }
}
