package net.axther.hydroxide.modules.navigation;

import java.util.function.Predicate;

public final class HomeLimitService {

    private HomeLimitService() {
    }

    public static int highestLimit(Predicate<String> permissionChecker, int defaultLimit, int maximumLimit) {
        int limit = Math.max(0, defaultLimit);
        int scanCeiling = Math.max(maximumLimit, 1000);
        for (int candidate = scanCeiling; candidate >= 0; candidate--) {
            if (permissionChecker.test("hydroxide.homes.limit." + candidate)) {
                return Math.min(candidate, maximumLimit);
            }
        }
        return Math.min(limit, maximumLimit);
    }
}
