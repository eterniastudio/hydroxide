package net.axther.hydroxide.modules.motd;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

final class ServerTimeFormatter {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private ServerTimeFormatter() {
    }

    static Snapshot snapshot(Instant instant, ZoneId zone) {
        ZoneId effectiveZone = zone == null ? ZoneId.systemDefault() : zone;
        return new Snapshot(
                FORMATTER.withZone(effectiveZone).format(instant),
                effectiveZone.getId(),
                FORMATTER.withZone(ZoneOffset.UTC).format(instant)
        );
    }

    record Snapshot(String localTime, String zone, String utcTime) {
    }
}
