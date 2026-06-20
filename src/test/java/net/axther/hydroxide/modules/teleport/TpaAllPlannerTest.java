package net.axther.hydroxide.modules.teleport;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TpaAllPlannerTest {

    @Test
    void excludesRequesterAndPlayersWithRequestsDisabled() {
        UUID requester = UUID.randomUUID();
        UUID enabled = UUID.randomUUID();
        UUID disabled = UUID.randomUUID();

        TpaAllPlanner.Plan plan = TpaAllPlanner.plan(requester, false, List.of(
                new TpaAllPlanner.Candidate(requester, true),
                new TpaAllPlanner.Candidate(enabled, true),
                new TpaAllPlanner.Candidate(disabled, false)
        ));

        assertEquals(List.of(enabled), plan.targetIds());
        assertEquals(1, plan.skippedDisabled());
    }

    @Test
    void bypassIncludesPlayersWithRequestsDisabled() {
        UUID requester = UUID.randomUUID();
        UUID disabled = UUID.randomUUID();

        TpaAllPlanner.Plan plan = TpaAllPlanner.plan(requester, true, List.of(
                new TpaAllPlanner.Candidate(requester, true),
                new TpaAllPlanner.Candidate(disabled, false)
        ));

        assertEquals(List.of(disabled), plan.targetIds());
        assertEquals(0, plan.skippedDisabled());
    }
}
