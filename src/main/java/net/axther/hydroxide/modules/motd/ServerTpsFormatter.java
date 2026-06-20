package net.axther.hydroxide.modules.motd;

import java.util.Locale;

final class ServerTpsFormatter {

    private ServerTpsFormatter() {
    }

    static Snapshot snapshot(double[] tps, double averageTickTime, long[] tickTimes) {
        return new Snapshot(
                formatTps(tps, 0),
                formatTps(tps, 1),
                formatTps(tps, 2),
                formatMillis(averageTickTime),
                formatMillis(worstTickNanos(tickTimes) / 1_000_000.0)
        );
    }

    private static String formatTps(double[] values, int index) {
        if (values.length <= index || !Double.isFinite(values[index]) || values[index] <= 0.0) {
            return "0.00";
        }
        if (values[index] > 20.0) {
            return "20.00*";
        }
        return String.format(Locale.ROOT, "%.2f", values[index]);
    }

    private static String formatMillis(double value) {
        if (!Double.isFinite(value) || value <= 0.0) {
            return "0.00";
        }
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private static long worstTickNanos(long[] tickTimes) {
        long worst = 0L;
        for (long tickTime : tickTimes) {
            worst = Math.max(worst, tickTime);
        }
        return worst;
    }

    record Snapshot(String oneMinuteTps, String fiveMinuteTps, String fifteenMinuteTps,
                    String averageMspt, String worstTickMs) {
    }
}
