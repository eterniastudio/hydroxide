package net.axther.hydroxide.modules.navigation;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HomeLimitServiceTest {

    @Test
    void picksHighestPermissionLimitOverDefault() {
        Set<String> permissions = Set.of("hydroxide.homes.limit.3", "hydroxide.homes.limit.10");

        int limit = HomeLimitService.highestLimit(permissions::contains, 1, 20);

        assertEquals(10, limit);
    }

    @Test
    void clampsPermissionLimitToConfiguredMaximum() {
        Set<String> permissions = Set.of("hydroxide.homes.limit.500");

        int limit = HomeLimitService.highestLimit(permissions::contains, 2, 25);

        assertEquals(25, limit);
    }
}
