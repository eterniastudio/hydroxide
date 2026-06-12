package net.axther.hydroxide.modules.staff;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VanishSettingsTest {

    @Test
    void defaultsDoNotAutoVanishOpsAndRestoreOnDisable() {
        VanishSettings settings = VanishSettings.from(new YamlConfiguration());

        assertTrue(settings.enabled());
        assertTrue(settings.persist());
        assertFalse(settings.autoVanishOps());
        assertTrue(settings.restoreVisibilityOnDisable());
        assertTrue(settings.staffCanSeeVanished());
        assertFalse(settings.debugVisibility());
    }

    @Test
    void readsConfiguredFlags() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("vanish.enabled", false);
        yaml.set("vanish.persist", false);
        yaml.set("vanish.auto-vanish-ops", true);
        yaml.set("vanish.restore-visibility-on-disable", false);
        yaml.set("vanish.staff-can-see", false);
        yaml.set("debug.visibility", true);

        VanishSettings settings = VanishSettings.from(yaml);

        assertFalse(settings.enabled());
        assertFalse(settings.persist());
        assertTrue(settings.autoVanishOps());
        assertFalse(settings.restoreVisibilityOnDisable());
        assertFalse(settings.staffCanSeeVanished());
        assertTrue(settings.debugVisibility());
    }
}
