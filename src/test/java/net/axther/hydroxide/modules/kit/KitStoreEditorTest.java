package net.axther.hydroxide.modules.kit;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KitStoreEditorTest {

    @Test
    void deletesKitAndMatchingCooldowns() {
        YamlConfiguration kits = new YamlConfiguration();
        YamlConfiguration cooldowns = new YamlConfiguration();
        UUID alex = UUID.randomUUID();
        UUID steve = UUID.randomUUID();
        kits.set("kits.starter.cooldown-seconds", 60L);
        cooldowns.set(alex + ".starter", 123L);
        cooldowns.set(alex + ".daily", 456L);
        cooldowns.set(steve + ".starter", 789L);

        assertTrue(KitStoreEditor.delete(kits, cooldowns, "starter"));

        assertFalse(kits.contains("kits.starter"));
        assertFalse(cooldowns.contains(alex + ".starter"));
        assertFalse(cooldowns.contains(steve + ".starter"));
        assertTrue(cooldowns.contains(alex + ".daily"));
    }

    @Test
    void returnsFalseWhenKitDoesNotExist() {
        assertFalse(KitStoreEditor.delete(new YamlConfiguration(), new YamlConfiguration(), "missing"));
    }
}
