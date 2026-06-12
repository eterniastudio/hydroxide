package net.axther.hydroxide.modules.staff;

import org.bukkit.configuration.ConfigurationSection;

public record VanishSettings(
        boolean enabled,
        boolean persist,
        boolean autoVanishOps,
        boolean restoreVisibilityOnDisable,
        boolean staffCanSeeVanished,
        boolean debugVisibility
) {

    public static VanishSettings from(ConfigurationSection config) {
        return new VanishSettings(
                config.getBoolean("vanish.enabled", true),
                config.getBoolean("vanish.persist", true),
                config.getBoolean("vanish.auto-vanish-ops", false),
                config.getBoolean("vanish.restore-visibility-on-disable", true),
                config.getBoolean("vanish.staff-can-see", true),
                config.getBoolean("debug.visibility", false)
        );
    }
}
