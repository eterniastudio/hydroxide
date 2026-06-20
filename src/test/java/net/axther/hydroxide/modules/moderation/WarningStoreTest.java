package net.axther.hydroxide.modules.moderation;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WarningStoreTest {

    @Test
    void storesWarningsAndRoundTripsThroughYaml() {
        UUID playerId = UUID.randomUUID();
        Instant now = Instant.parse("2026-06-15T12:00:00Z");
        WarningStore store = new WarningStore();

        store.add(new WarningRecord(playerId, "Example", "Console", "First warning", now));

        YamlConfiguration yaml = new YamlConfiguration();
        store.writeTo(yaml);
        WarningStore loaded = WarningStore.from(yaml);

        List<WarningRecord> warnings = loaded.warnings(playerId);
        assertEquals(1, warnings.size());
        assertEquals("First warning", warnings.getFirst().reason());
    }

    @Test
    void clearRemovesWarningsForOnePlayer() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        WarningStore store = new WarningStore();
        store.add(new WarningRecord(first, "One", "Console", "First", Instant.now()));
        store.add(new WarningRecord(second, "Two", "Console", "Second", Instant.now()));

        assertEquals(1, store.clear(first));

        assertTrue(store.warnings(first).isEmpty());
        assertEquals(1, store.warnings(second).size());
    }

    @Test
    void clearAllRemovesEveryWarningAndReturnsCount() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        WarningStore store = new WarningStore();
        store.add(new WarningRecord(first, "One", "Console", "First", Instant.now()));
        store.add(new WarningRecord(first, "One", "Console", "Another", Instant.now()));
        store.add(new WarningRecord(second, "Two", "Console", "Second", Instant.now()));

        assertEquals(3, store.clearAll());

        assertTrue(store.warnings(first).isEmpty());
        assertTrue(store.warnings(second).isEmpty());
    }
}
