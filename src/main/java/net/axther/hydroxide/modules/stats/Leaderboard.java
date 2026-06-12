package net.axther.hydroxide.modules.stats;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class Leaderboard {

    private final Map<UUID, Long> values;

    public Leaderboard(Map<UUID, Long> values) {
        this.values = Map.copyOf(values);
    }

    public List<Entry> top(int limit) {
        return values.entrySet().stream()
                .sorted(Map.Entry.<UUID, Long>comparingByValue(Comparator.reverseOrder()))
                .limit(Math.max(0, limit))
                .map(entry -> new Entry(entry.getKey(), entry.getValue()))
                .toList();
    }

    public record Entry(UUID playerId, long value) {
    }
}
