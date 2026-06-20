package net.axther.hydroxide.teleport;

import org.bukkit.entity.Player;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class TeleportRequestService {

    private final Map<UUID, TeleportRequest> requestsByTarget = new ConcurrentHashMap<>();
    private final Clock clock;

    public TeleportRequestService() {
        this(Clock.systemUTC());
    }

    TeleportRequestService(Clock clock) {
        this.clock = clock;
    }

    public void request(Player requester, Player target, Duration timeout) {
        request(
                requester.getUniqueId(),
                requester.getName(),
                target.getUniqueId(),
                target.getName(),
                TeleportRequestDirection.REQUESTER_TO_TARGET,
                timeout
        );
    }

    public void requestHere(Player requester, Player target, Duration timeout) {
        request(
                requester.getUniqueId(),
                requester.getName(),
                target.getUniqueId(),
                target.getName(),
                TeleportRequestDirection.TARGET_TO_REQUESTER,
                timeout
        );
    }

    void request(UUID requesterId, String requesterName, UUID targetId, String targetName,
                 TeleportRequestDirection direction, Duration timeout) {
        requestsByTarget.put(targetId, new TeleportRequest(
                requesterId,
                requesterName,
                targetId,
                targetName,
                direction,
                clock.instant().plus(timeout)
        ));
    }

    public Optional<TeleportRequest> pendingFor(Player target) {
        return pendingFor(target.getUniqueId());
    }

    Optional<TeleportRequest> pendingFor(UUID targetId) {
        TeleportRequest request = requestsByTarget.get(targetId);
        if (request == null) {
            return Optional.empty();
        }
        if (request.expiresAt().isBefore(clock.instant())) {
            requestsByTarget.remove(targetId);
            return Optional.empty();
        }
        return Optional.of(request);
    }

    public Optional<TeleportRequest> accept(Player target) {
        return pendingFor(target).map(request -> {
            requestsByTarget.remove(target.getUniqueId());
            return request;
        });
    }

    public Optional<TeleportRequest> deny(Player target) {
        return accept(target);
    }

    public Optional<TeleportRequest> cancel(Player requester) {
        return cancel(playerId(requester));
    }

    Optional<TeleportRequest> cancel(UUID requesterId) {
        return requestsByTarget.entrySet().stream()
                .filter(entry -> entry.getValue().requesterId().equals(requesterId))
                .findFirst()
                .map(entry -> {
                    requestsByTarget.remove(entry.getKey());
                    return entry.getValue();
                });
    }

    private UUID playerId(Player player) {
        return player.getUniqueId();
    }

    public enum TeleportRequestDirection {
        REQUESTER_TO_TARGET,
        TARGET_TO_REQUESTER
    }

    public record TeleportRequest(
            UUID requesterId,
            String requesterName,
            UUID targetId,
            String targetName,
            TeleportRequestDirection direction,
            Instant expiresAt
    ) {
    }
}
