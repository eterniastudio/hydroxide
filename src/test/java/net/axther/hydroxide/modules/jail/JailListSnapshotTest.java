package net.axther.hydroxide.modules.jail;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JailListSnapshotTest {

    private static final Instant NOW = Instant.parse("2026-06-16T12:00:00Z");
    private static final UUID STEVE_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID ALEX_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");

    @Test
    void buildsConfiguredCellsWithOccupancyAndPrisonerDetails() {
        JailListSnapshot snapshot = JailListSnapshot.create(
                List.of("spawn-2", "spawn", "mine"),
                List.of(
                        sentence(STEVE_ID, "spawn", "griefing", 300),
                        sentence(ALEX_ID, "spawn-2", "spam", 120)
                ),
                Map.of(STEVE_ID, "Steve", ALEX_ID, "Alex"),
                Optional.empty(),
                Optional.empty(),
                NOW
        );

        assertEquals(3, snapshot.cells().size());
        assertEquals("mine", snapshot.cells().get(0).jailName());
        assertEquals(0, snapshot.cells().get(0).prisonerCount());
        assertEquals("spawn", snapshot.cells().get(1).jailName());
        assertEquals(1, snapshot.cells().get(1).prisonerCount());
        assertEquals("Steve", snapshot.cells().get(1).prisoners().getFirst().playerName());
        assertEquals(300, snapshot.cells().get(1).prisoners().getFirst().remainingSeconds());
        assertEquals("griefing", snapshot.cells().get(1).prisoners().getFirst().reason());
        assertEquals("spawn-2", snapshot.cells().get(2).jailName());
        assertEquals("Alex", snapshot.cells().get(2).prisoners().getFirst().playerName());
    }

    @Test
    void filtersByCmiJailNameAndCellId() {
        JailListSnapshot snapshot = JailListSnapshot.create(
                List.of("main", "main-2", "other"),
                List.of(
                        sentence(STEVE_ID, "main", "reason", 60),
                        sentence(ALEX_ID, "main-2", "reason", 60)
                ),
                Map.of(STEVE_ID, "Steve", ALEX_ID, "Alex"),
                Optional.of("main"),
                Optional.of("2"),
                NOW
        );

        assertEquals(1, snapshot.cells().size());
        assertEquals("main-2", snapshot.cells().getFirst().jailName());
        assertEquals("Alex", snapshot.cells().getFirst().prisoners().getFirst().playerName());
    }

    @Test
    void includesHyphenatedCellsWhenFilteringByJailNameOnly() {
        JailListSnapshot snapshot = JailListSnapshot.create(
                List.of("main", "main-2", "other"),
                List.of(),
                Map.of(),
                Optional.of("MAIN"),
                Optional.empty(),
                NOW
        );

        assertEquals(List.of("main", "main-2"), snapshot.cells().stream().map(JailListSnapshot.Cell::jailName).toList());
    }

    @Test
    void fallsBackToUuidWhenNameIsUnknown() {
        JailListSnapshot snapshot = JailListSnapshot.create(
                List.of("main"),
                List.of(sentence(STEVE_ID, "main", "reason", 60)),
                Map.of(),
                Optional.empty(),
                Optional.empty(),
                NOW
        );

        assertTrue(snapshot.cells().getFirst().prisoners().getFirst().playerName().contains(STEVE_ID.toString()));
    }

    private static JailSentence sentence(UUID playerId, String jailName, String reason, long remainingSeconds) {
        return new JailSentence(
                playerId,
                jailName,
                UUID.fromString("00000000-0000-0000-0000-000000000099"),
                reason,
                NOW.plusSeconds(remainingSeconds),
                null
        );
    }
}
