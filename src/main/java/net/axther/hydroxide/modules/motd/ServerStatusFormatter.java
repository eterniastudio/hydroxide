package net.axther.hydroxide.modules.motd;

final class ServerStatusFormatter {

    private ServerStatusFormatter() {
    }

    static Snapshot snapshot(long uptimeMillis, long maxMemoryBytes, long allocatedMemoryBytes, long freeMemoryBytes,
                             int worlds, int onlinePlayers, int maxPlayers, double[] tps, double averageTickTime) {
        ServerGcFormatter.Snapshot memory = ServerGcFormatter.snapshot(
                maxMemoryBytes,
                allocatedMemoryBytes,
                freeMemoryBytes,
                worlds,
                onlinePlayers,
                maxPlayers
        );
        ServerTpsFormatter.Snapshot performance = ServerTpsFormatter.snapshot(tps, averageTickTime, new long[0]);
        return new Snapshot(
                formatUptime(uptimeMillis),
                memory.usedMemoryMb(),
                memory.maxMemoryMb(),
                memory.worlds(),
                memory.onlinePlayers(),
                memory.maxPlayers(),
                performance.oneMinuteTps(),
                performance.averageMspt()
        );
    }

    private static String formatUptime(long uptimeMillis) {
        long seconds = Math.max(0L, uptimeMillis / 1000L);
        long days = seconds / 86_400L;
        seconds %= 86_400L;
        long hours = seconds / 3_600L;
        seconds %= 3_600L;
        long minutes = seconds / 60L;
        seconds %= 60L;

        if (days > 0L) {
            return days + "d " + hours + "h " + minutes + "m " + seconds + "s";
        }
        if (hours > 0L) {
            return hours + "h " + minutes + "m " + seconds + "s";
        }
        if (minutes > 0L) {
            return minutes + "m " + seconds + "s";
        }
        return seconds + "s";
    }

    record Snapshot(String uptime, long usedMemoryMb, long maxMemoryMb, int worlds,
                    int onlinePlayers, int maxPlayers, String oneMinuteTps, String averageMspt) {
    }
}
