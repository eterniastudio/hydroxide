package net.axther.hydroxide.modules.nickname;

import net.axther.hydroxide.storage.YamlStore;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

final class NameplateStore {

    private final Map<UUID, StoredNameplate> entries = new HashMap<>();

    static NameplateStore from(YamlConfiguration yaml) {
        NameplateStore store = new NameplateStore();
        ConfigurationSection players = yaml.getConfigurationSection("players");
        if (players == null) {
            return store;
        }
        for (String key : players.getKeys(false)) {
            try {
                UUID playerId = UUID.fromString(key);
                String playerName = yaml.getString("players." + key + ".name", "Unknown");
                String prefix = yaml.getString("players." + key + ".prefix", "");
                String suffix = yaml.getString("players." + key + ".suffix", "");
                NamedTextColor color = NameplateCommandParser.parseColor(yaml.getString("players." + key + ".color", ""))
                        .orElse(null);
                store.put(playerId, playerName, new NicknameService.NameplateState(prefix, suffix, color));
            } catch (IllegalArgumentException ignored) {
                // Ignore malformed UUID keys so one bad entry does not block all nameplates.
            }
        }
        return store;
    }

    Optional<StoredNameplate> get(UUID playerId) {
        return Optional.ofNullable(entries.get(playerId));
    }

    Map<UUID, StoredNameplate> entries() {
        return Map.copyOf(entries);
    }

    void put(UUID playerId, String playerName, NicknameService.NameplateState state) {
        if (state.empty()) {
            remove(playerId);
            return;
        }
        entries.put(playerId, new StoredNameplate(playerId, playerName, state));
    }

    void remove(UUID playerId) {
        entries.remove(playerId);
    }

    void save(YamlStore store) {
        store.save(toYaml());
    }

    YamlConfiguration toYaml() {
        YamlConfiguration yaml = new YamlConfiguration();
        entries.values().forEach(entry -> {
            String path = "players." + entry.playerId();
            yaml.set(path + ".name", entry.playerName());
            yaml.set(path + ".prefix", entry.state().prefix());
            yaml.set(path + ".suffix", entry.state().suffix());
            entry.state().color().ifPresent(color ->
                    yaml.set(path + ".color", NameplateCommandParser.colorName(color)));
        });
        return yaml;
    }

    record StoredNameplate(UUID playerId, String playerName, NicknameService.NameplateState state) {
    }
}
