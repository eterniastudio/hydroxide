package net.axther.hydroxide.storage;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class NamedLocationStore {

    private final YamlStore yamlStore;
    private final String sectionName;

    public NamedLocationStore(File file, String sectionName) {
        this.yamlStore = new YamlStore(file);
        this.sectionName = sectionName;
    }

    public void set(String name, StoredLocation location) {
        YamlConfiguration yaml = yamlStore.load();
        ConfigurationSection section = yaml.createSection(path(name));
        location.writeTo(section);
        yamlStore.save(yaml);
    }

    public Optional<StoredLocation> get(String name) {
        YamlConfiguration yaml = yamlStore.load();
        return StoredLocation.readFrom(yaml.getConfigurationSection(path(name)));
    }

    public boolean remove(String name) {
        YamlConfiguration yaml = yamlStore.load();
        String path = path(name);
        if (!yaml.contains(path)) {
            return false;
        }
        yaml.set(path, null);
        yamlStore.save(yaml);
        return true;
    }

    public List<String> names() {
        YamlConfiguration yaml = yamlStore.load();
        ConfigurationSection section = yaml.getConfigurationSection(sectionName);
        if (section == null) {
            return List.of();
        }
        return section.getKeys(false).stream().sorted(String.CASE_INSENSITIVE_ORDER).toList();
    }

    private String path(String name) {
        return sectionName + "." + normalize(name);
    }

    private String normalize(String name) {
        return name.toLowerCase(Locale.ROOT);
    }
}
