package net.axther.hydroxide.modules.economy;

import net.axther.hydroxide.storage.YamlStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChequeRedemptionStoreTest {

    @TempDir
    Path tempDir;

    @Test
    void persistsRedeemedChequeIds() {
        UUID chequeId = UUID.randomUUID();
        ChequeRedemptionStore store = store();

        assertFalse(store.redeemed(chequeId));

        store.markRedeemed(chequeId);

        assertTrue(store().redeemed(chequeId));
    }

    private ChequeRedemptionStore store() {
        return new ChequeRedemptionStore(new YamlStore(tempDir.resolve("cheques.yml").toFile()));
    }
}
