package net.axther.hydroxide.modules.jail;

import net.axther.hydroxide.storage.StoredLocation;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

public record JailSentence(
        UUID playerId,
        String jailName,
        UUID jailerId,
        String reason,
        Instant releaseAt,
        StoredLocation previousLocation
) {

    public long remainingSeconds(Instant now) {
        return Math.max(0L, Duration.between(now, releaseAt).toSeconds());
    }

    public boolean expired(Instant now) {
        return !releaseAt.isAfter(now);
    }
}
