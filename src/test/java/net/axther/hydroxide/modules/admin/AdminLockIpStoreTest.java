package net.axther.hydroxide.modules.admin;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdminLockIpStoreTest {

    @Test
    void allowsPlayersWithoutLockedIpHashes() {
        AdminLockIpStore store = new AdminLockIpStore(new YamlConfiguration());

        assertTrue(store.isAllowed(UUID.randomUUID(), "hash-a"));
    }

    @Test
    void addsUniqueHashesAndRequiresOneToMatch() {
        AdminLockIpStore store = new AdminLockIpStore(new YamlConfiguration());
        UUID playerId = UUID.randomUUID();

        assertTrue(store.add(playerId, "hash-a"));
        assertFalse(store.add(playerId, "hash-a"));

        assertEquals(List.of("hash-a"), store.hashes(playerId));
        assertTrue(store.isAllowed(playerId, "hash-a"));
        assertFalse(store.isAllowed(playerId, "hash-b"));
    }

    @Test
    void removesAndClearsHashes() {
        AdminLockIpStore store = new AdminLockIpStore(new YamlConfiguration());
        UUID playerId = UUID.randomUUID();

        store.add(playerId, "hash-a");
        store.add(playerId, "hash-b");

        assertTrue(store.remove(playerId, "hash-a"));
        assertFalse(store.remove(playerId, "hash-a"));
        assertEquals(List.of("hash-b"), store.hashes(playerId));

        store.clear(playerId);
        assertTrue(store.hashes(playerId).isEmpty());
        assertTrue(store.isAllowed(playerId, "hash-a"));
    }
}
