package net.axther.hydroxide.modules.admin;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdminOperatorListFormatterTest {

    @Test
    void sortsOperatorsCaseInsensitivelyAndTracksOnlineState() {
        AdminOperatorListFormatter.Snapshot snapshot = AdminOperatorListFormatter.snapshot(List.of(
                new AdminOperatorListFormatter.Entry("zeta", false),
                new AdminOperatorListFormatter.Entry("Alex", true),
                new AdminOperatorListFormatter.Entry("bravo", false)
        ));

        assertEquals(3, snapshot.count());
        assertEquals(List.of("Alex", "bravo", "zeta"), snapshot.entries().stream()
                .map(AdminOperatorListFormatter.Entry::name)
                .toList());
        assertEquals("admin.oplist.state.online", snapshot.entries().getFirst().stateKey());
        assertEquals("admin.oplist.state.offline", snapshot.entries().get(1).stateKey());
    }

    @Test
    void handlesEmptyAndBlankOperatorNames() {
        AdminOperatorListFormatter.Snapshot empty = AdminOperatorListFormatter.snapshot(List.of());
        AdminOperatorListFormatter.Snapshot withBlank = AdminOperatorListFormatter.snapshot(List.of(
                new AdminOperatorListFormatter.Entry(" ", false)
        ));

        assertTrue(empty.entries().isEmpty());
        assertEquals("unknown", withBlank.entries().getFirst().name());
    }
}
