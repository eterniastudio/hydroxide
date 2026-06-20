package net.axther.hydroxide.modules.moderation;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

final class WarningStore {

    private final Map<UUID, List<WarningRecord>> warnings = new HashMap<>();

    static WarningStore from(YamlConfiguration yaml) {
        WarningStore store = new WarningStore();
        ConfigurationSection players = yaml.getConfigurationSection("warnings");
        if (players == null) {
            return store;
        }
        for (String playerKey : players.getKeys(false)) {
            try {
                UUID playerId = UUID.fromString(playerKey);
                ConfigurationSection entries = yaml.getConfigurationSection("warnings." + playerKey + ".entries");
                if (entries == null) {
                    continue;
                }
                for (String entryKey : entries.getKeys(false)) {
                    String path = "warnings." + playerKey + ".entries." + entryKey;
                    store.add(new WarningRecord(
                            playerId,
                            yaml.getString("warnings." + playerKey + ".name", playerKey),
                            yaml.getString(path + ".issuer", "unknown"),
                            yaml.getString(path + ".reason", "Warning"),
                            Instant.parse(yaml.getString(path + ".created-at", Instant.EPOCH.toString()))
                    ));
                }
            } catch (RuntimeException ignored) {
                // Ignore malformed warning entries so moderation history remains usable.
            }
        }
        return store;
    }

    void writeTo(YamlConfiguration yaml) {
        yaml.set("warnings", null);
        for (Map.Entry<UUID, List<WarningRecord>> entry : warnings.entrySet()) {
            UUID playerId = entry.getKey();
            List<WarningRecord> records = entry.getValue();
            if (records.isEmpty()) {
                continue;
            }
            yaml.set("warnings." + playerId + ".name", records.getFirst().playerName());
            for (int i = 0; i < records.size(); i++) {
                WarningRecord warning = records.get(i);
                String path = "warnings." + playerId + ".entries." + i;
                yaml.set(path + ".issuer", warning.issuer());
                yaml.set(path + ".reason", warning.reason());
                yaml.set(path + ".created-at", warning.createdAt().toString());
            }
        }
    }

    void add(WarningRecord record) {
        warnings.computeIfAbsent(record.playerId(), ignored -> new ArrayList<>()).add(record);
    }

    List<WarningRecord> warnings(UUID playerId) {
        return warnings.getOrDefault(playerId, List.of()).stream()
                .sorted(Comparator.comparing(WarningRecord::createdAt))
                .toList();
    }

    int clear(UUID playerId) {
        List<WarningRecord> removed = warnings.remove(playerId);
        return removed == null ? 0 : removed.size();
    }

    int clearAll() {
        int count = warnings.values().stream().mapToInt(List::size).sum();
        warnings.clear();
        return count;
    }
}
