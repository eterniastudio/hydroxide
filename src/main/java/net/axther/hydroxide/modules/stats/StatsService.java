package net.axther.hydroxide.modules.stats;

import net.axther.hydroxide.storage.YamlStore;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class StatsService {

    private final YamlStore store;

    public StatsService(YamlStore store) {
        this.store = store;
    }

    public void rememberName(UUID playerId, String name) {
        YamlConfiguration yaml = store.load();
        yaml.set(path(playerId, "name"), name);
        store.save(yaml);
    }

    public void increment(UUID playerId, String stat, long amount) {
        YamlConfiguration yaml = store.load();
        String path = path(playerId, normalize(stat));
        yaml.set(path, yaml.getLong(path, 0L) + amount);
        store.save(yaml);
    }

    public void set(UUID playerId, String stat, long value) {
        YamlConfiguration yaml = store.load();
        yaml.set(path(playerId, normalize(stat)), Math.max(0L, value));
        store.save(yaml);
    }

    public long value(UUID playerId, String stat) {
        return store.load().getLong(path(playerId, normalize(stat)), 0L);
    }

    public Optional<String> name(UUID playerId) {
        return Optional.ofNullable(store.load().getString(path(playerId, "name")));
    }

    public Optional<UUID> playerId(String name) {
        if (name == null || name.isBlank()) {
            return Optional.empty();
        }
        String normalized = name.toLowerCase(Locale.ROOT);
        YamlConfiguration yaml = store.load();
        ConfigurationSection players = yaml.getConfigurationSection("players");
        if (players == null) {
            return Optional.empty();
        }
        for (String key : players.getKeys(false)) {
            String storedName = yaml.getString("players." + key + ".name");
            if (storedName != null && storedName.toLowerCase(Locale.ROOT).equals(normalized)) {
                try {
                    return Optional.of(UUID.fromString(key));
                } catch (IllegalArgumentException ignored) {
                    return Optional.empty();
                }
            }
        }
        return Optional.empty();
    }

    public Map<UUID, Long> values(String stat) {
        YamlConfiguration yaml = store.load();
        ConfigurationSection players = yaml.getConfigurationSection("players");
        if (players == null) {
            return Map.of();
        }
        Map<UUID, Long> values = new HashMap<>();
        for (String key : players.getKeys(false)) {
            try {
                UUID playerId = UUID.fromString(key);
                values.put(playerId, yaml.getLong("players." + key + "." + normalize(stat), 0L));
            } catch (IllegalArgumentException ignored) {
                // Ignore malformed UUID sections.
            }
        }
        return Map.copyOf(values);
    }

    private String path(UUID playerId, String key) {
        return "players." + playerId + "." + key;
    }

    private String normalize(String stat) {
        return stat.toLowerCase(Locale.ROOT).replace("-", "_");
    }
}
