package net.axther.hydroxide.modules.economy;

import java.math.BigDecimal;
import java.util.Optional;

final class ChequeAmountParser {

    private ChequeAmountParser() {
    }

    static Optional<Double> parse(String input) {
        if (input == null || input.isBlank()) {
            return Optional.empty();
        }
        try {
            BigDecimal amount = new BigDecimal(input.trim());
            double value = amount.doubleValue();
            if (amount.scale() > 2 || amount.signum() <= 0 || Double.isNaN(value) || Double.isInfinite(value)) {
                return Optional.empty();
            }
            return Optional.of(value);
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }
    }

    static String canonical(double amount) {
        return BigDecimal.valueOf(amount)
                .setScale(2, java.math.RoundingMode.UNNECESSARY)
                .toPlainString();
    }
}
