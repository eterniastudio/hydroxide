package net.axther.hydroxide.modules;

import org.bukkit.configuration.file.FileConfiguration;

import java.util.Map;

@FunctionalInterface
public interface ModuleConfig {

    boolean isEnabled(String moduleId, boolean defaultEnabled);

    static ModuleConfig overrides(Map<String, Boolean> overrides) {
        return (moduleId, defaultEnabled) -> overrides.getOrDefault(moduleId, defaultEnabled);
    }

    static ModuleConfig fromConfiguration(FileConfiguration configuration) {
        return (moduleId, defaultEnabled) -> configuration.getBoolean("modules." + moduleId + ".enabled", defaultEnabled);
    }
}
