package net.axther.hydroxide.modules.teleport;

import net.axther.hydroxide.storage.YamlStore;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.UUID;

final class TeleportPreferenceStore {

    private static final String ROOT = "players";

    private final YamlStore store;

    TeleportPreferenceStore(YamlStore store) {
        this.store = store;
    }

    boolean requestsEnabled(UUID playerId) {
        return store.load().getBoolean(path(playerId, "requests-enabled"), true);
    }

    void setRequestsEnabled(UUID playerId, boolean enabled) {
        set(playerId, "requests-enabled", enabled);
    }

    boolean autoAcceptEnabled(UUID playerId) {
        return store.load().getBoolean(path(playerId, "auto-accept"), false);
    }

    void setAutoAcceptEnabled(UUID playerId, boolean enabled) {
        set(playerId, "auto-accept", enabled);
    }

    private void set(UUID playerId, String key, boolean enabled) {
        YamlConfiguration yaml = store.load();
        yaml.set(path(playerId, key), enabled);
        store.save(yaml);
    }

    private String path(UUID playerId, String key) {
        return ROOT + "." + playerId + "." + key;
    }
}
