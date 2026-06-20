package net.axther.hydroxide.teleport;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TeleportRequestServiceTest {

    private final Clock clock = Clock.fixed(Instant.parse("2026-06-15T12:00:00Z"), ZoneOffset.UTC);

    @Test
    void storesTeleportRequestDirection() {
        TeleportRequestService service = new TeleportRequestService(clock);
        UUID requester = UUID.randomUUID();
        UUID target = UUID.randomUUID();

        service.request(requester, "Alice", target, "Bob",
                TeleportRequestService.TeleportRequestDirection.REQUESTER_TO_TARGET,
                Duration.ofSeconds(30));

        TeleportRequestService.TeleportRequest request = service.pendingFor(target).orElseThrow();
        assertEquals(TeleportRequestService.TeleportRequestDirection.REQUESTER_TO_TARGET, request.direction());
        assertEquals(requester, request.requesterId());
        assertEquals(target, request.targetId());
    }

    @Test
    void cancelsPendingRequestByRequester() {
        TeleportRequestService service = new TeleportRequestService(clock);
        UUID requester = UUID.randomUUID();
        UUID target = UUID.randomUUID();
        service.request(requester, "Alice", target, "Bob",
                TeleportRequestService.TeleportRequestDirection.TARGET_TO_REQUESTER,
                Duration.ofSeconds(30));

        TeleportRequestService.TeleportRequest cancelled = service.cancel(requester).orElseThrow();

        assertEquals(TeleportRequestService.TeleportRequestDirection.TARGET_TO_REQUESTER, cancelled.direction());
        assertTrue(service.pendingFor(target).isEmpty());
        assertTrue(service.cancel(requester).isEmpty());
    }
}
