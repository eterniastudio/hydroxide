package net.axther.hydroxide.modules.teleport;

import net.axther.hydroxide.storage.StoredLocation;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeathLocationStoreTest {

    @Test
    void storesAndReadsLastDeathLocationByPlayerId() {
        UUID playerId = UUID.fromString("00000000-0000-0000-0000-000000000123");
        StoredLocation location = new StoredLocation("world", 12.5, 64.0, -3.25, 90.0f, 15.0f);
        YamlConfiguration yaml = new YamlConfiguration();

        DeathLocationStore.write(yaml, playerId, "Alex", location);

        assertEquals(location, DeathLocationStore.read(yaml, playerId).orElseThrow());
        assertEquals("Alex", yaml.getString("players." + playerId + ".name"));
        assertTrue(DeathLocationStore.read(yaml, UUID.randomUUID()).isEmpty());
    }
}
