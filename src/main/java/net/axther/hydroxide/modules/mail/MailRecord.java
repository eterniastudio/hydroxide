package net.axther.hydroxide.modules.mail;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

record MailRecord(UUID id, UUID recipient, String senderName, String message, Instant createdAt,
                  Optional<Instant> expiresAt) {

    MailRecord(UUID id, UUID recipient, String senderName, String message, Instant createdAt) {
        this(id, recipient, senderName, message, createdAt, Optional.empty());
    }

    MailRecord {
        expiresAt = expiresAt == null ? Optional.empty() : expiresAt;
    }

    boolean expired(Instant now) {
        return expiresAt.map(expiration -> !expiration.isAfter(now)).orElse(false);
    }
}
