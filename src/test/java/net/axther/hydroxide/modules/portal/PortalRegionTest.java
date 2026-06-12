package net.axther.hydroxide.modules.portal;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PortalRegionTest {

    @Test
    void containsLocationsInsideInclusiveBoundsOnlyForMatchingWorld() {
        PortalRegion region = new PortalRegion("spawn", "world", 0, 60, 0, 10, 80, 10, "hub");

        assertTrue(region.contains("world", 5, 70, 5));
        assertTrue(region.contains("world", 0, 60, 0));
        assertFalse(region.contains("world_nether", 5, 70, 5));
        assertFalse(region.contains("world", 11, 70, 5));
    }
}
