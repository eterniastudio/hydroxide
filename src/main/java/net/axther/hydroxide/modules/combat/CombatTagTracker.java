package net.axther.hydroxide.modules.combat;

import java.util.HashMap;
import java.util.Map;
import java.util.OptionalLong;
import java.util.UUID;

public final class CombatTagTracker {

    private final long durationMillis;
    private final Map<UUID, Long> taggedUntil = new HashMap<>();

    public CombatTagTracker(long durationMillis) {
        this.durationMillis = Math.max(1000L, durationMillis);
    }

    public void tag(UUID firstPlayer, UUID secondPlayer, long nowMillis) {
        long until = nowMillis + durationMillis;
        taggedUntil.put(firstPlayer, until);
        taggedUntil.put(secondPlayer, until);
    }

    public boolean tagged(UUID playerId, long nowMillis) {
        Long until = taggedUntil.get(playerId);
        if (until == null) {
            return false;
        }
        if (nowMillis > until) {
            taggedUntil.remove(playerId);
            return false;
        }
        return true;
    }

    public OptionalLong remainingMillis(UUID playerId, long nowMillis) {
        if (!tagged(playerId, nowMillis)) {
            return OptionalLong.empty();
        }
        return OptionalLong.of(Math.max(0L, taggedUntil.get(playerId) - nowMillis));
    }

    public void clear(UUID playerId) {
        taggedUntil.remove(playerId);
    }
}
