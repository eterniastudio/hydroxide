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
        requestsByTarget.put(target.getUniqueId(), new TeleportRequest(
                requester.getUniqueId(),
                requester.getName(),
                target.getUniqueId(),
                target.getName(),
                clock.instant().plus(timeout)
        ));
    }

    public Optional<TeleportRequest> pendingFor(Player target) {
        TeleportRequest request = requestsByTarget.get(target.getUniqueId());
        if (request == null) {
            return Optional.empty();
        }
        if (request.expiresAt().isBefore(clock.instant())) {
            requestsByTarget.remove(target.getUniqueId());
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

    public record TeleportRequest(
            UUID requesterId,
            String requesterName,
            UUID targetId,
            String targetName,
            Instant expiresAt
    ) {
    }
}
