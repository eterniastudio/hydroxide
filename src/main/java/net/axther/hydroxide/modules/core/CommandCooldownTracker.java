package net.axther.hydroxide.modules.core;

import java.time.Duration;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

final class CommandCooldownTracker {

    private final Map<Key, Long> readyAt = new HashMap<>();

    Optional<ActiveCooldown> checkAndMark(UUID playerId, CommandCooldownPolicy.Cooldown cooldown, long nowMillis) {
        Key key = new Key(playerId, cooldown.key());
        long existingReadyAt = readyAt.getOrDefault(key, 0L);
        if (existingReadyAt > nowMillis) {
            if (existingReadyAt == Long.MAX_VALUE) {
                return Optional.of(new ActiveCooldown(cooldown.key(), Duration.ZERO, true));
            }
            return Optional.of(new ActiveCooldown(cooldown.key(), Duration.ofMillis(existingReadyAt - nowMillis), false));
        }
        readyAt.put(key, cooldown.oneUse() ? Long.MAX_VALUE : nowMillis + cooldown.duration().toMillis());
        return Optional.empty();
    }

    void restore(List<Entry> entries, long nowMillis) {
        readyAt.clear();
        for (Entry entry : entries) {
            if (entry.readyAtMillis() > nowMillis && entry.playerId() != null && entry.key() != null && !entry.key().isBlank()) {
                readyAt.put(new Key(entry.playerId(), entry.key()), entry.readyAtMillis());
            }
        }
    }

    List<Entry> snapshot(long nowMillis) {
        return readyAt.entrySet().stream()
                .filter(entry -> entry.getValue() > nowMillis)
                .map(entry -> new Entry(entry.getKey().playerId(), entry.getKey().commandKey(), entry.getValue()))
                .sorted(Comparator.comparing((Entry entry) -> entry.playerId().toString()).thenComparing(Entry::key))
                .toList();
    }

    int clear(UUID playerId, String commandKey) {
        String normalizedKey = normalizeKey(commandKey);
        int before = readyAt.size();
        readyAt.entrySet().removeIf(entry -> entry.getKey().playerId().equals(playerId)
                && (normalizedKey.isBlank() || entry.getKey().commandKey().equals(normalizedKey)));
        return before - readyAt.size();
    }

    int clearAll(String commandKey) {
        String normalizedKey = normalizeKey(commandKey);
        int before = readyAt.size();
        readyAt.entrySet().removeIf(entry -> normalizedKey.isBlank() || entry.getKey().commandKey().equals(normalizedKey));
        return before - readyAt.size();
    }

    int activeCount(long nowMillis) {
        return snapshot(nowMillis).size();
    }

    private String normalizeKey(String commandKey) {
        return commandKey == null ? "" : commandKey.trim().toLowerCase(java.util.Locale.ROOT);
    }

    private record Key(UUID playerId, String commandKey) {
    }

    record ActiveCooldown(String key, Duration remaining, boolean oneUse) {
    }

    record Entry(UUID playerId, String key, long readyAtMillis) {
    }
}
