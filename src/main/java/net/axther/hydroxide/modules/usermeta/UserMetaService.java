package net.axther.hydroxide.modules.usermeta;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class UserMetaService {

    private final UserMetaStore store;

    public UserMetaService(UserMetaStore store) {
        this.store = store;
    }

    public Optional<String> value(UUID playerId, String key) {
        return store.get(playerId, normalizeKey(key));
    }

    public Optional<String> integerValue(UUID playerId, String key) {
        return value(playerId, key).flatMap(value -> {
            try {
                return Optional.of(new BigDecimal(value).toBigInteger().toString());
            } catch (NumberFormatException exception) {
                return Optional.empty();
            }
        });
    }

    Map<String, String> all(UUID playerId) {
        return store.all(playerId);
    }

    void set(UUID playerId, String playerName, String key, String value) {
        store.set(playerId, playerName, normalizeKey(key), value);
    }

    boolean remove(UUID playerId, String key) {
        return store.remove(playerId, normalizeKey(key));
    }

    int clear(UUID playerId) {
        return store.clear(playerId);
    }

    Optional<Double> increment(UUID playerId, String playerName, String key, double delta) {
        return store.increment(playerId, playerName, normalizeKey(key), delta);
    }

    public static String normalizeKey(String key) {
        if (key == null) {
            return "";
        }
        return key.trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9_.-]", "_");
    }
}
