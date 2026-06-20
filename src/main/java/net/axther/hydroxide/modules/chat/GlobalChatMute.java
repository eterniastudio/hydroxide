package net.axther.hydroxide.modules.chat;

import java.time.Duration;
import java.time.Instant;

record GlobalChatMute(String issuer, String reason, Instant createdAt, Instant expiresAt) {

    boolean expired(Instant now) {
        return !expiresAt.isAfter(now);
    }

    Duration remaining(Instant now) {
        return expired(now) ? Duration.ZERO : Duration.between(now, expiresAt);
    }
}
