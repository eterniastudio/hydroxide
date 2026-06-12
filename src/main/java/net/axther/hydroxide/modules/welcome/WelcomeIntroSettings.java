package net.axther.hydroxide.modules.welcome;

import org.bukkit.configuration.ConfigurationSection;

public record WelcomeIntroSettings(boolean introEnabled) {

    public static WelcomeIntroSettings from(ConfigurationSection yaml) {
        boolean welcomeScreenEnabled = yaml.getBoolean("welcome-screen.enabled", true);
        boolean welcomeEnabled = yaml.getBoolean("welcome.enabled", welcomeScreenEnabled);
        boolean introEnabled = yaml.getBoolean("welcome.intro.enabled", welcomeScreenEnabled);
        boolean legacyIntroEnabled = yaml.getBoolean("intro.enabled", true);
        return new WelcomeIntroSettings(welcomeScreenEnabled && welcomeEnabled && introEnabled && legacyIntroEnabled);
    }
}
