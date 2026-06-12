package net.axther.hydroxide.modules.builder;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BrushLimitsTest {

    @Test
    void clampsRadiusAndChecksVolume() {
        BrushLimits limits = new BrushLimits(6, 500);

        assertEquals(6, limits.clampRadius(20));
        assertTrue(limits.withinBlockLimit(10));
        assertFalse(limits.withinBlockLimit(501));
    }
}
