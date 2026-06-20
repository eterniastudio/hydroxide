package net.axther.hydroxide.modules.moderation;

import java.time.Instant;
import java.util.UUID;

record MuteRecord(UUID playerId, String playerName, String issuer, String reason, Instant createdAt, Instant expiresAt) {

    boolean expired(Instant now) {
        return expiresAt != null && !expiresAt.isAfter(now);
    }

    String remaining(Instant now) {
        if (expiresAt == null) {
            return "permanent";
        }
        long seconds = Math.max(0L, expiresAt.getEpochSecond() - now.getEpochSecond());
        if (seconds >= 86400L) {
            return (seconds / 86400L) + "d";
        }
        if (seconds >= 3600L) {
            return (seconds / 3600L) + "h";
        }
        if (seconds >= 60L) {
            return (seconds / 60L) + "m";
        }
        return seconds + "s";
    }
}
