package net.axther.hydroxide.modules.moderation;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

final class MuteStore {

    private final Map<UUID, MuteRecord> mutes = new HashMap<>();

    static MuteStore from(YamlConfiguration yaml) {
        MuteStore store = new MuteStore();
        ConfigurationSection section = yaml.getConfigurationSection("mutes");
        if (section == null) {
            return store;
        }
        for (String key : section.getKeys(false)) {
            try {
                UUID playerId = UUID.fromString(key);
                String path = "mutes." + key;
                store.mute(new MuteRecord(
                        playerId,
                        yaml.getString(path + ".name", key),
                        yaml.getString(path + ".issuer", "unknown"),
                        yaml.getString(path + ".reason", "Muted"),
                        Instant.parse(yaml.getString(path + ".created-at", Instant.EPOCH.toString())),
                        parseInstant(yaml.getString(path + ".expires-at", ""))
                ));
            } catch (IllegalArgumentException ignored) {
                // Ignore malformed mute entries so one bad record does not break moderation.
            }
        }
        return store;
    }

    void writeTo(YamlConfiguration yaml) {
        yaml.set("mutes", null);
        for (MuteRecord mute : mutes.values()) {
            String path = "mutes." + mute.playerId();
            yaml.set(path + ".name", mute.playerName());
            yaml.set(path + ".issuer", mute.issuer());
            yaml.set(path + ".reason", mute.reason());
            yaml.set(path + ".created-at", mute.createdAt().toString());
            yaml.set(path + ".expires-at", mute.expiresAt() == null ? null : mute.expiresAt().toString());
        }
    }

    void mute(MuteRecord record) {
        mutes.put(record.playerId(), record);
    }

    boolean unmute(UUID playerId) {
        return mutes.remove(playerId) != null;
    }

    boolean contains(UUID playerId) {
        return mutes.containsKey(playerId);
    }

    Optional<MuteRecord> active(UUID playerId, Instant now) {
        MuteRecord mute = mutes.get(playerId);
        return mute == null || mute.expired(now) ? Optional.empty() : Optional.of(mute);
    }

    boolean pruneExpired(Instant now) {
        int before = mutes.size();
        mutes.values().removeIf(mute -> mute.expired(now));
        return mutes.size() != before;
    }

    private static Instant parseInstant(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return Instant.parse(value);
    }
}
