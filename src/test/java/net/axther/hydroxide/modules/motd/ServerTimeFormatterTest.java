package net.axther.hydroxide.modules.motd;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ServerTimeFormatterTest {

    @Test
    void formatsServerAndUtcTimeFromFixedInstant() {
        ServerTimeFormatter.Snapshot snapshot = ServerTimeFormatter.snapshot(
                Instant.parse("2026-06-16T18:42:30Z"),
                ZoneId.of("America/New_York")
        );

        assertEquals("2026-06-16 14:42:30", snapshot.localTime());
        assertEquals("America/New_York", snapshot.zone());
        assertEquals("2026-06-16 18:42:30", snapshot.utcTime());
    }
}
