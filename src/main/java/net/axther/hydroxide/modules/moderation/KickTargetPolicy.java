package net.axther.hydroxide.modules.moderation;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

final class KickTargetPolicy {

    static final String BYPASS_PERMISSION = "hydroxide.command.kick.bypass";

    private KickTargetPolicy() {
    }

    static <T> Selection<T> selectKickable(List<T> candidates, Predicate<T> bypassPredicate) {
        List<T> kickable = new ArrayList<>();
        List<T> bypassed = new ArrayList<>();
        for (T candidate : candidates) {
            if (bypassPredicate.test(candidate)) {
                bypassed.add(candidate);
            } else {
                kickable.add(candidate);
            }
        }
        return new Selection<>(List.copyOf(kickable), List.copyOf(bypassed));
    }

    record Selection<T>(List<T> kickable, List<T> bypassed) {
    }
}
