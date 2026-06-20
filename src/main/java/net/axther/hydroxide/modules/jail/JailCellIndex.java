package net.axther.hydroxide.modules.jail;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.List;
import java.util.Locale;

final class JailCellIndex {

    private JailCellIndex() {
    }

    static List<String> names(YamlConfiguration yaml) {
        ConfigurationSection section = yaml.getConfigurationSection("jails");
        return section == null ? List.of() : section.getKeys(false).stream()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    static boolean delete(YamlConfiguration yaml, String jailName) {
        String normalized = jailName.toLowerCase(Locale.ROOT);
        String path = "jails." + normalized;
        if (!yaml.isConfigurationSection(path)) {
            return false;
        }
        yaml.set(path, null);
        return true;
    }
}
