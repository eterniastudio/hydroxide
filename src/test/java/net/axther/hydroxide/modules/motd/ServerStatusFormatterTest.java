package net.axther.hydroxide.modules.motd;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ServerStatusFormatterTest {

    @Test
    void summarizesServerHealthInputs() {
        long mb = 1024L * 1024L;

        ServerStatusFormatter.Snapshot snapshot = ServerStatusFormatter.snapshot(
                3_661_000L,
                1024L * mb,
                512L * mb,
                128L * mb,
                4,
                27,
                100,
                new double[]{19.965D, 20.0D, 18.5D},
                35.678D
        );

        assertEquals("1h 1m 1s", snapshot.uptime());
        assertEquals(384L, snapshot.usedMemoryMb());
        assertEquals(1024L, snapshot.maxMemoryMb());
        assertEquals(4, snapshot.worlds());
        assertEquals(27, snapshot.onlinePlayers());
        assertEquals(100, snapshot.maxPlayers());
        assertEquals("19.97", snapshot.oneMinuteTps());
        assertEquals("35.68", snapshot.averageMspt());
    }

    @Test
    void formatsShortUptime() {
        ServerStatusFormatter.Snapshot snapshot = ServerStatusFormatter.snapshot(
                42_000L,
                0L,
                0L,
                0L,
                0,
                0,
                0,
                new double[0],
                0.0D
        );

        assertEquals("42s", snapshot.uptime());
    }
}
