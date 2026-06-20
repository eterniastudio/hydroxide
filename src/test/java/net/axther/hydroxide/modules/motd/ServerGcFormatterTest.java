package net.axther.hydroxide.modules.motd;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ServerGcFormatterTest {

    @Test
    void calculatesMegabyteMemorySnapshot() {
        long mb = 1024L * 1024L;

        ServerGcFormatter.Snapshot snapshot = ServerGcFormatter.snapshot(
                1024L * mb,
                512L * mb,
                128L * mb,
                3,
                12,
                100
        );

        assertEquals(384, snapshot.usedMemoryMb());
        assertEquals(512, snapshot.allocatedMemoryMb());
        assertEquals(1024, snapshot.maxMemoryMb());
        assertEquals(3, snapshot.worlds());
        assertEquals(12, snapshot.onlinePlayers());
        assertEquals(100, snapshot.maxPlayers());
    }
}
