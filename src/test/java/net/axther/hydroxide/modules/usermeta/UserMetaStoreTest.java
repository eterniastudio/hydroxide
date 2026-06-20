package net.axther.hydroxide.modules.usermeta;

import net.axther.hydroxide.storage.YamlStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserMetaStoreTest {

    @TempDir
    Path tempDir;

    @Test
    void storesListsRemovesAndClearsPlayerMetadata() {
        UserMetaStore store = store();
        UUID playerId = UUID.randomUUID();

        store.set(playerId, "Steve", "rank", "admin");
        store.set(playerId, "Steve", "score", "12");

        assertEquals("admin", store.get(playerId, "rank").orElseThrow());
        assertEquals(Map.of("rank", "admin", "score", "12"), store.all(playerId));

        assertTrue(store.remove(playerId, "rank"));
        assertTrue(store.get(playerId, "rank").isEmpty());
        assertEquals(1, store.clear(playerId));
        assertTrue(store.all(playerId).isEmpty());
    }

    @Test
    void incrementsNumericMetadataAndKeepsExistingNonNumericValuesSafe() {
        UserMetaStore store = store();
        UUID playerId = UUID.randomUUID();

        assertEquals(1.25D, store.increment(playerId, "Alex", "score", 1.25D).orElseThrow(), 0.0001D);
        assertEquals("1.25", store.get(playerId, "score").orElseThrow());
        assertEquals(3.0D, store.increment(playerId, "Alex", "score", 1.75D).orElseThrow(), 0.0001D);
        assertEquals("3", store.get(playerId, "score").orElseThrow());

        store.set(playerId, "Alex", "rank", "admin");
        assertTrue(store.increment(playerId, "Alex", "rank", 1.0D).isEmpty());
        assertEquals("admin", store.get(playerId, "rank").orElseThrow());
    }

    private UserMetaStore store() {
        return new UserMetaStore(new YamlStore(tempDir.resolve("user-meta.yml").toFile()));
    }
}
