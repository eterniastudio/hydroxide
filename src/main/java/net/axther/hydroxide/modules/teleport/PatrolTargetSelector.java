package net.axther.hydroxide.modules.teleport;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.IntUnaryOperator;

final class PatrolTargetSelector {

    private final Map<String, Set<String>> visitedByPatroller = new HashMap<>();
    private final IntUnaryOperator indexPicker;

    PatrolTargetSelector() {
        this(size -> ThreadLocalRandom.current().nextInt(size));
    }

    PatrolTargetSelector(IntUnaryOperator indexPicker) {
        this.indexPicker = indexPicker;
    }

    Optional<String> next(String patrollerName, List<String> onlineNames) {
        List<String> candidates = candidates(patrollerName, onlineNames);
        if (candidates.isEmpty()) {
            return Optional.empty();
        }

        Set<String> visited = visitedByPatroller.computeIfAbsent(normalize(patrollerName), ignored -> new HashSet<>());
        List<String> unseen = candidates.stream()
                .filter(candidate -> !visited.contains(normalize(candidate)))
                .toList();
        if (unseen.isEmpty()) {
            visited.clear();
            unseen = candidates;
        }

        String selected = unseen.get(Math.floorMod(indexPicker.applyAsInt(unseen.size()), unseen.size()));
        visited.add(normalize(selected));
        return Optional.of(selected);
    }

    void reset(String patrollerName) {
        visitedByPatroller.remove(normalize(patrollerName));
    }

    private List<String> candidates(String patrollerName, List<String> onlineNames) {
        List<String> candidates = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (String onlineName : onlineNames) {
            if (onlineName.equalsIgnoreCase(patrollerName)) {
                continue;
            }
            if (seen.add(normalize(onlineName))) {
                candidates.add(onlineName);
            }
        }
        return candidates;
    }

    private String normalize(String value) {
        return value.toLowerCase(Locale.ROOT);
    }
}
