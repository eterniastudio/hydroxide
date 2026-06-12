package net.axther.hydroxide.storage.database;

import net.axther.hydroxide.storage.PlayerDataStore;
import net.axther.hydroxide.storage.SqlPlayerDataStore;
import net.axther.hydroxide.storage.StoredLocation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StorageEngineTest {

    @TempDir
    Path tempDir;

    @Test
    void persistsProfilesHomesAndFriendsThroughSqlite() throws Exception {
        DatabaseSettings settings = DatabaseSettings.sqlite("test.db");
        UUID playerId = UUID.randomUUID();
        UUID friendId = UUID.randomUUID();
        StoredLocation home = new StoredLocation("world", 12.5, 70.0, -9.25, 90.0f, 15.0f);

        try (DatabaseManager manager = DatabaseManager.open(settings, tempDir.toFile())) {
            StorageEngine engine = new StorageEngine(manager);

            engine.saveProfile(playerId, "Kevin", "<#44CCFF>Kev", 250.75).get(5, TimeUnit.SECONDS);
            engine.saveHome(playerId, "Base", home).get(5, TimeUnit.SECONDS);
            engine.addFriend(playerId, friendId).get(5, TimeUnit.SECONDS);

            assertEquals(250.75, engine.getBalance(playerId, 0.0).get(5, TimeUnit.SECONDS));
            assertEquals(Optional.of("<#44CCFF>Kev"), engine.getNickname(playerId).get(5, TimeUnit.SECONDS));
            assertEquals(home, engine.getHome(playerId, "base").get(5, TimeUnit.SECONDS).orElseThrow());
            assertEquals(List.of("base"), engine.homes(playerId).get(5, TimeUnit.SECONDS));
            assertEquals(List.of(friendId), engine.friends(playerId).get(5, TimeUnit.SECONDS));
            assertEquals("Kevin", engine.nicknames().get(5, TimeUnit.SECONDS).get(playerId).playerName());
        }
    }

    @Test
    void sqlPlayerDataStoreKeepsCompatibilityWithExistingSynchronousCalls() throws Exception {
        DatabaseSettings settings = DatabaseSettings.sqlite("sync.db");
        UUID playerId = UUID.randomUUID();
        UUID friendId = UUID.randomUUID();
        StoredLocation home = new StoredLocation("world_nether", 1.0, 64.0, 2.0, 180.0f, 0.0f);

        try (DatabaseManager manager = DatabaseManager.open(settings, tempDir.toFile())) {
            PlayerDataStore store = new SqlPlayerDataStore(new StorageEngine(manager));

            store.setNickname(playerId, "Axther", "&bHydroxide");
            store.setBalance(playerId, 90.25);
            store.setHome(playerId, "Vault", home);
            store.addFriend(playerId, friendId);

            assertEquals(Optional.of("&bHydroxide"), store.nickname(playerId));
            assertEquals(90.25, store.balance(playerId, 100.0));
            assertEquals(Optional.of(home), store.home(playerId, "vault"));
            assertEquals(List.of("vault"), store.homes(playerId));
            assertEquals(List.of(friendId), store.friends(playerId));
            assertTrue(store.nicknames().containsKey(playerId));
        }
    }

    @Test
    void rejectsUnsafeBalancesBeforeWriting() throws Exception {
        DatabaseSettings settings = DatabaseSettings.sqlite("balances.db");

        try (DatabaseManager manager = DatabaseManager.open(settings, tempDir.toFile())) {
            PlayerDataStore store = new SqlPlayerDataStore(new StorageEngine(manager));
            UUID playerId = UUID.randomUUID();

            assertThrows(IllegalArgumentException.class, () -> store.setBalance(playerId, Double.NaN));
            assertThrows(IllegalArgumentException.class, () -> store.setBalance(playerId, Double.POSITIVE_INFINITY));
            assertThrows(IllegalArgumentException.class, () -> store.setBalance(playerId, 1.001));
        }
    }
}
