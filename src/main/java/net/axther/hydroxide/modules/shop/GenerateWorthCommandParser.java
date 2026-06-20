package net.axther.hydroxide.modules.shop;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

final class GenerateWorthCommandParser {

    private static final double DEFAULT_BASE_PRICE = 1.0D;

    private GenerateWorthCommandParser() {
    }

    static Optional<Request> parse(List<String> args) {
        double basePrice = DEFAULT_BASE_PRICE;
        boolean overwrite = false;
        boolean baseSeen = false;
        for (String raw : args) {
            String token = raw.toLowerCase(Locale.ROOT);
            if (token.equals("-overwrite") || token.equals("--overwrite") || token.equals("overwrite")) {
                overwrite = true;
                continue;
            }
            if (baseSeen) {
                return Optional.empty();
            }
            try {
                double parsed = Double.parseDouble(raw);
                if (!ShopPricing.validMoney(parsed) || parsed <= 0.0D) {
                    return Optional.empty();
                }
                basePrice = parsed;
                baseSeen = true;
            } catch (NumberFormatException exception) {
                return Optional.empty();
            }
        }
        return Optional.of(new Request(basePrice, overwrite));
    }

    record Request(double basePrice, boolean overwrite) {
    }
}
