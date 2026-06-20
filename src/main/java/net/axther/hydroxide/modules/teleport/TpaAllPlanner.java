package net.axther.hydroxide.modules.teleport;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

final class TpaAllPlanner {

    private TpaAllPlanner() {
    }

    static Plan plan(UUID requesterId, boolean bypassDisabledRequests, List<Candidate> candidates) {
        List<UUID> targetIds = new ArrayList<>();
        int skippedDisabled = 0;
        for (Candidate candidate : candidates) {
            if (candidate.playerId().equals(requesterId)) {
                continue;
            }
            if (!candidate.requestsEnabled() && !bypassDisabledRequests) {
                skippedDisabled++;
                continue;
            }
            targetIds.add(candidate.playerId());
        }
        return new Plan(List.copyOf(targetIds), skippedDisabled);
    }

    record Candidate(UUID playerId, boolean requestsEnabled) {
    }

    record Plan(List<UUID> targetIds, int skippedDisabled) {
    }
}
