package net.axther.hydroxide.modules.jail;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JailCellIndexTest {

    @Test
    void listsJailCellsInStableOrder() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.createSection("jails.zeta");
        yaml.createSection("jails.alpha");

        assertEquals(List.of("alpha", "zeta"), JailCellIndex.names(yaml));
    }

    @Test
    void deletesExistingJailCellByNormalizedName() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.createSection("jails.spawn");

        assertTrue(JailCellIndex.delete(yaml, "Spawn"));
        assertFalse(yaml.isConfigurationSection("jails.spawn"));
    }

    @Test
    void reportsMissingJailCellWithoutChangingYaml() {
        YamlConfiguration yaml = new YamlConfiguration();

        assertFalse(JailCellIndex.delete(yaml, "missing"));
        assertTrue(JailCellIndex.names(yaml).isEmpty());
    }
}
