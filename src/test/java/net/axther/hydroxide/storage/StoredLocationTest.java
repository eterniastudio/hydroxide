package net.axther.hydroxide.storage;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StoredLocationTest {

    @Test
    void roundTripsThroughConfigurationSection() {
        StoredLocation stored = new StoredLocation("world_nether", 12.5, 70.0, -31.25, 180.0f, 12.5f);
        YamlConfiguration yaml = new YamlConfiguration();

        stored.writeTo(yaml.createSection("spawn"));

        assertEquals(stored, StoredLocation.readFrom(yaml.getConfigurationSection("spawn")).orElseThrow());
    }

    @Test
    void missingWorldMakesLocationInvalid() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("x", 1.0);
        yaml.set("y", 2.0);
        yaml.set("z", 3.0);

        assertTrue(StoredLocation.readFrom(yaml).isEmpty());
    }
}
