package net.axther.hydroxide.modules.usermeta;

import net.axther.hydroxide.storage.YamlStore;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

final class UserMetaStore {

    private static final char PATH_SEPARATOR = '/';

    private final YamlStore store;

    UserMetaStore(YamlStore store) {
        this.store = store;
    }

    Optional<String> get(UUID playerId, String key) {
        return Optional.ofNullable(load().getString(metaPath(playerId, key)));
    }

    Map<String, String> all(UUID playerId) {
        ConfigurationSection section = load().getConfigurationSection(metaRoot(playerId));
        if (section == null) {
            return Map.of();
        }
        Map<String, String> entries = new LinkedHashMap<>();
        for (String encodedKey : section.getKeys(false).stream().sorted(String.CASE_INSENSITIVE_ORDER).toList()) {
            ConfigurationSection entry = section.getConfigurationSection(encodedKey);
            if (entry != null && entry.isString("key") && entry.isString("value")) {
                entries.put(entry.getString("key"), entry.getString("value"));
            }
        }
        return Collections.unmodifiableMap(entries);
    }

    void set(UUID playerId, String playerName, String key, String value) {
        YamlConfiguration yaml = load();
        rememberName(yaml, playerId, playerName);
        yaml.set(metaEntryPath(playerId, key) + "/key", UserMetaService.normalizeKey(key));
        yaml.set(metaEntryPath(playerId, key) + "/value", value);
        store.save(yaml);
    }

    boolean remove(UUID playerId, String key) {
        YamlConfiguration yaml = load();
        String path = metaEntryPath(playerId, key);
        if (!yaml.isSet(path)) {
            return false;
        }
        yaml.set(path, null);
        store.save(yaml);
        return true;
    }

    int clear(UUID playerId) {
        YamlConfiguration yaml = load();
        ConfigurationSection section = yaml.getConfigurationSection(metaRoot(playerId));
        if (section == null) {
            return 0;
        }
        int removed = section.getKeys(false).size();
        yaml.set(metaRoot(playerId), null);
        store.save(yaml);
        return removed;
    }

    Optional<Double> increment(UUID playerId, String playerName, String key, double delta) {
        if (!Double.isFinite(delta)) {
            return Optional.empty();
        }
        YamlConfiguration yaml = load();
        String path = metaEntryPath(playerId, key);
        BigDecimal current;
        try {
            current = new BigDecimal(yaml.getString(path + "/value", "0"));
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }
        BigDecimal next = current.add(BigDecimal.valueOf(delta));
        rememberName(yaml, playerId, playerName);
        yaml.set(path + "/key", UserMetaService.normalizeKey(key));
        yaml.set(path + "/value", format(next));
        store.save(yaml);
        return Optional.of(next.doubleValue());
    }

    private YamlConfiguration load() {
        YamlConfiguration yaml = store.load();
        yaml.options().pathSeparator(PATH_SEPARATOR);
        return yaml;
    }

    private void rememberName(YamlConfiguration yaml, UUID playerId, String playerName) {
        if (playerName != null && !playerName.isBlank()) {
            yaml.set("players/" + playerId + "/name", playerName);
        }
    }

    private static String metaRoot(UUID playerId) {
        return "players/" + playerId + "/meta";
    }

    private static String metaPath(UUID playerId, String key) {
        return metaEntryPath(playerId, key) + "/value";
    }

    private static String metaEntryPath(UUID playerId, String key) {
        String normalized = UserMetaService.normalizeKey(key);
        return metaRoot(playerId) + "/" + Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(normalized.getBytes(StandardCharsets.UTF_8));
    }

    private static String format(BigDecimal value) {
        BigDecimal stripped = value.stripTrailingZeros();
        if (stripped.scale() < 0) {
            stripped = stripped.setScale(0);
        }
        return stripped.toPlainString();
    }
}
