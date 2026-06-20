package net.axther.hydroxide.modules.motd;

final class ServerGcFormatter {

    private static final long BYTES_PER_MEGABYTE = 1024L * 1024L;

    private ServerGcFormatter() {
    }

    static Snapshot snapshot(long maxMemoryBytes, long allocatedMemoryBytes, long freeMemoryBytes,
                             int worlds, int onlinePlayers, int maxPlayers) {
        long usedMemoryBytes = Math.max(0L, allocatedMemoryBytes - freeMemoryBytes);
        return new Snapshot(
                toMegabytes(usedMemoryBytes),
                toMegabytes(allocatedMemoryBytes),
                toMegabytes(maxMemoryBytes),
                worlds,
                onlinePlayers,
                maxPlayers
        );
    }

    private static long toMegabytes(long bytes) {
        return Math.max(0L, bytes / BYTES_PER_MEGABYTE);
    }

    record Snapshot(long usedMemoryMb, long allocatedMemoryMb, long maxMemoryMb,
                    int worlds, int onlinePlayers, int maxPlayers) {
    }
}
