package dojo;

import java.time.Duration;

/** Back-of-the-envelope capacity arithmetic. Every count is rounded up. */
public final class Estimates {
    private Estimates() {}

    /** Average requests per second over a 86,400-second day. */
    public static long requestsPerSecond(long dailyUsers, long requestsPerUserPerDay) {
        throw new UnsupportedOperationException("todo requests per second");
    }

    /** The peak load: average × factor (factor at least 1). */
    public static long peak(long average, double factor) {
        throw new UnsupportedOperationException("todo peak");
    }

    /** Total bytes stored over {@code days}, every replica counted. */
    public static long storageBytes(long itemsPerDay, long bytesPerItem, int days, int replicas) {
        throw new UnsupportedOperationException("todo storage");
    }

    /** Servers so that none runs above {@code maxUtilizationPercent} at peak, plus one spare. */
    public static long serversNeeded(long peakRps, long rpsPerServer, int maxUtilizationPercent) {
        throw new UnsupportedOperationException("todo servers");
    }

    /** Little's law: requests in flight = throughput × latency. */
    public static long inFlight(long rps, Duration latency) {
        throw new UnsupportedOperationException("todo in flight");
    }
}
