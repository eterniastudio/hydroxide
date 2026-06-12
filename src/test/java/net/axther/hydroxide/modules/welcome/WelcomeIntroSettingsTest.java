package net.axther.hydroxide.modules.welcome;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WelcomeIntroSettingsTest {

    @Test
    void disablesIntroWhenAnySupportedFlagIsFalse() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("welcome-screen.enabled", true);
        assertTrue(WelcomeIntroSettings.from(yaml).introEnabled());

        yaml.set("welcome.enabled", false);
        assertFalse(WelcomeIntroSettings.from(yaml).introEnabled());

        yaml.set("welcome.enabled", true);
        yaml.set("welcome.intro.enabled", false);
        assertFalse(WelcomeIntroSettings.from(yaml).introEnabled());

        yaml.set("welcome.intro.enabled", true);
        yaml.set("welcome-screen.enabled", false);
        assertFalse(WelcomeIntroSettings.from(yaml).introEnabled());
    }
}
