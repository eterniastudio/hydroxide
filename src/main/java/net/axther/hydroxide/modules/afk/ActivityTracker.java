package net.axther.hydroxide.modules.afk;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class ActivityTracker {

    private final long idleMillis;
    private final Map<UUID, Long> lastActivity = new HashMap<>();

    public ActivityTracker(long idleMillis) {
        this.idleMillis = Math.max(1000L, idleMillis);
    }

    public void recordActivity(UUID playerId, long nowMillis) {
        lastActivity.put(playerId, nowMillis);
    }

    public boolean afk(UUID playerId, long nowMillis) {
        Long last = lastActivity.get(playerId);
        return last != null && nowMillis - last >= idleMillis;
    }

    public void clear(UUID playerId) {
        lastActivity.remove(playerId);
    }
}
