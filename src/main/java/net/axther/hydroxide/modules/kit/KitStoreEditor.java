package net.axther.hydroxide.modules.kit;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.Locale;

final class KitStoreEditor {

    private KitStoreEditor() {
    }

    static boolean delete(YamlConfiguration kits, YamlConfiguration cooldowns, String kitName) {
        String normalized = kitName.toLowerCase(Locale.ROOT);
        String kitPath = "kits." + normalized;
        if (!kits.contains(kitPath)) {
            return false;
        }

        kits.set(kitPath, null);
        for (String playerId : cooldowns.getKeys(false)) {
            ConfigurationSection section = cooldowns.getConfigurationSection(playerId);
            if (section != null && section.contains(normalized)) {
                cooldowns.set(playerId + "." + normalized, null);
            }
        }
        return true;
    }
}
