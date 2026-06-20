package net.axther.hydroxide.modules.motd;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ServerTpsFormatterTest {

    @Test
    void formatsTpsMsptAndWorstTickSamples() {
        ServerTpsFormatter.Snapshot snapshot = ServerTpsFormatter.snapshot(
                new double[]{20.453, 19.876, 17.345},
                42.345,
                new long[]{10_000_000L, 75_500_000L, 52_250_000L}
        );

        assertEquals("20.00*", snapshot.oneMinuteTps());
        assertEquals("19.88", snapshot.fiveMinuteTps());
        assertEquals("17.35", snapshot.fifteenMinuteTps());
        assertEquals("42.35", snapshot.averageMspt());
        assertEquals("75.50", snapshot.worstTickMs());
    }

    @Test
    void handlesMissingTpsAndTickSamples() {
        ServerTpsFormatter.Snapshot snapshot = ServerTpsFormatter.snapshot(new double[0], 0.0, new long[0]);

        assertEquals("0.00", snapshot.oneMinuteTps());
        assertEquals("0.00", snapshot.fiveMinuteTps());
        assertEquals("0.00", snapshot.fifteenMinuteTps());
        assertEquals("0.00", snapshot.averageMspt());
        assertEquals("0.00", snapshot.worstTickMs());
    }
}
