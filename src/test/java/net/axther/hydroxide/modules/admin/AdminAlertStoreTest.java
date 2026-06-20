package net.axther.hydroxide.modules.admin;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdminAlertStoreTest {

    @Test
    void addsFindsListsAndRemovesAlerts() {
        YamlConfiguration yaml = new YamlConfiguration();
        AdminAlertStore store = new AdminAlertStore(yaml);
        UUID playerId = UUID.randomUUID();

        store.add(playerId, "Steve", "Admin", "watch login");

        AdminAlertStore.Alert alert = store.find(playerId).orElseThrow();
        assertEquals(playerId, alert.playerId());
        assertEquals("Steve", alert.playerName());
        assertEquals("Admin", alert.issuer());
        assertEquals("watch login", alert.reason());
        assertEquals(List.of(alert), store.list());

        assertTrue(store.remove(playerId));
        assertTrue(store.find(playerId).isEmpty());
    }

    @Test
    void updatingExistingAlertKeepsSingleRecord() {
        YamlConfiguration yaml = new YamlConfiguration();
        AdminAlertStore store = new AdminAlertStore(yaml);
        UUID playerId = UUID.randomUUID();

        store.add(playerId, "Steve", "Admin", "first");
        store.add(playerId, "Steve", "Owner", "second");

        assertEquals(1, store.list().size());
        AdminAlertStore.Alert alert = store.find(playerId).orElseThrow();
        assertEquals("Owner", alert.issuer());
        assertEquals("second", alert.reason());
    }
}
