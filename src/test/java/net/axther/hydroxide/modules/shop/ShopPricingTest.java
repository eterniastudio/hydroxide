package net.axther.hydroxide.modules.shop;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShopPricingTest {

    @Test
    void validatesFiniteTwoDecimalPricesAndCalculatesTotals() {
        assertTrue(ShopPricing.validMoney(12.34));
        assertFalse(ShopPricing.validMoney(Double.NaN));
        assertFalse(ShopPricing.validMoney(Double.POSITIVE_INFINITY));
        assertFalse(ShopPricing.validMoney(1.001));
        assertEquals(37.02, ShopPricing.total(12.34, 3));
    }
}
