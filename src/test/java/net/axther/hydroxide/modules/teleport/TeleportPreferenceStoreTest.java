package net.axther.hydroxide.modules.teleport;

import net.axther.hydroxide.storage.YamlStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TeleportPreferenceStoreTest {

    @TempDir
    Path tempDir;

    @Test
    void defaultsToReceivingRequestsWithoutAutoAccept() {
        TeleportPreferenceStore store = store();
        UUID player = UUID.randomUUID();

        assertTrue(store.requestsEnabled(player));
        assertFalse(store.autoAcceptEnabled(player));
    }

    @Test
    void persistsRequestAndAutoAcceptFlags() {
        TeleportPreferenceStore store = store();
        UUID player = UUID.randomUUID();

        store.setRequestsEnabled(player, false);
        store.setAutoAcceptEnabled(player, true);

        TeleportPreferenceStore reloaded = store();
        assertFalse(reloaded.requestsEnabled(player));
        assertTrue(reloaded.autoAcceptEnabled(player));

        reloaded.setRequestsEnabled(player, true);
        reloaded.setAutoAcceptEnabled(player, false);

        TeleportPreferenceStore finalRead = store();
        assertTrue(finalRead.requestsEnabled(player));
        assertFalse(finalRead.autoAcceptEnabled(player));
    }

    private TeleportPreferenceStore store() {
        return new TeleportPreferenceStore(new YamlStore(tempDir.resolve("teleport-preferences.yml").toFile()));
    }
}
