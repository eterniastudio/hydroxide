package net.axther.hydroxide.modules.shop;

import java.math.BigDecimal;

public final class ShopPricing {

    private ShopPricing() {
    }

    public static boolean validMoney(double amount) {
        return amount >= 0.0
                && !Double.isNaN(amount)
                && !Double.isInfinite(amount)
                && BigDecimal.valueOf(amount).scale() <= 2;
    }

    public static double total(double unitPrice, int quantity) {
        if (!validMoney(unitPrice) || quantity < 1) {
            throw new IllegalArgumentException("Invalid shop price or quantity");
        }
        return BigDecimal.valueOf(unitPrice)
                .multiply(BigDecimal.valueOf(quantity))
                .setScale(2, java.math.RoundingMode.HALF_UP)
                .doubleValue();
    }
}
