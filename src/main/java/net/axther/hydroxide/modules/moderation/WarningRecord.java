package net.axther.hydroxide.modules.moderation;

import java.time.Instant;
import java.util.UUID;

record WarningRecord(UUID playerId, String playerName, String issuer, String reason, Instant createdAt) {
}
