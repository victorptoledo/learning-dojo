package dojo;

import java.time.Duration;

/** Back-of-the-envelope capacity arithmetic. Every count is rounded up. */
public final class Estimates {
    private static final long SECONDS_PER_DAY = 86_400;

    private Estimates() {}

    /** Average requests per second over a 86,400-second day. */
    public static long requestsPerSecond(long dailyUsers, long requestsPerUserPerDay) {
        requireNotNegative(dailyUsers, "daily users");
        requireNotNegative(requestsPerUserPerDay, "requests per user");
        return Math.ceilDiv(Math.multiplyExact(dailyUsers, requestsPerUserPerDay), SECONDS_PER_DAY);
    }

    /** The peak load: average × factor (factor at least 1). */
    public static long peak(long average, double factor) {
        requireNotNegative(average, "average");
        if (!(factor >= 1)) {
            throw new IllegalArgumentException("the peak factor must be at least 1");
        }
        return (long) Math.ceil(average * factor);
    }

    /** Total bytes stored over {@code days}, every replica counted. */
    public static long storageBytes(long itemsPerDay, long bytesPerItem, int days, int replicas) {
        requireNotNegative(itemsPerDay, "items per day");
        requireNotNegative(bytesPerItem, "bytes per item");
        requireNotNegative(days, "days");
        requireNotNegative(replicas, "replicas");
        long perDay = Math.multiplyExact(itemsPerDay, bytesPerItem);
        return Math.multiplyExact(Math.multiplyExact(perDay, (long) days), (long) replicas);
    }

    /** Servers so that none runs above {@code maxUtilizationPercent} at peak, plus one spare. */
    public static long serversNeeded(long peakRps, long rpsPerServer, int maxUtilizationPercent) {
        requireNotNegative(peakRps, "peak");
        if (rpsPerServer < 1) {
            throw new IllegalArgumentException("a server must handle at least 1 request per second");
        }
        if (maxUtilizationPercent < 1 || maxUtilizationPercent > 100) {
            throw new IllegalArgumentException("utilization must be between 1 and 100 percent");
        }
        long usable = Math.multiplyExact(rpsPerServer, (long) maxUtilizationPercent);
        return Math.ceilDiv(Math.multiplyExact(peakRps, 100L), usable) + 1;
    }

    /** Little's law: requests in flight = throughput × latency. */
    public static long inFlight(long rps, Duration latency) {
        requireNotNegative(rps, "requests per second");
        if (latency.isNegative()) {
            throw new IllegalArgumentException("latency cannot be negative");
        }
        return Math.ceilDiv(Math.multiplyExact(rps, latency.toNanos()), 1_000_000_000L);
    }

    private static void requireNotNegative(long value, String what) {
        if (value < 0) {
            throw new IllegalArgumentException(what + " cannot be negative");
        }
    }
}
