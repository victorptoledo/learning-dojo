package dojo.shortener;

import java.time.Clock;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/** The public face of the shortener, with token-bucket rate limits per client. See README.md. */
public final class Gateway {

    /**
     * A token bucket kept in nanoseconds of refill, so continuous refill stays exact: one
     * token is `refillNanos`, a full bucket is `burst * refillNanos`.
     */
    private static final class Bucket {
        private final long refillNanos;
        private final long capacity;
        private long level;
        private long updatedAt;

        Bucket(int burst, Duration refill, long now) {
            refillNanos = refill.toNanos();
            capacity = burst * refillNanos;
            level = capacity;
            updatedAt = now;
        }

        void refill(long now) {
            level = Math.min(capacity, level + (now - updatedAt));
            updatedAt = now;
        }

        boolean isFull(long now) {
            refill(now);
            return level == capacity;
        }

        /** 0 when a token was taken, otherwise the nanoseconds until there is one. */
        long take(long now) {
            refill(now);
            if (level >= refillNanos) {
                level -= refillNanos;
                return 0;
            }
            return refillNanos - level;
        }
    }

    private record Client(Bucket writes, Bucket reads) {}

    private final Shortener shortener;
    private final Clock clock;
    private final Limits limits;
    private final long sweepEveryNanos;
    private final Map<String, Client> clients = new HashMap<>();
    private long lastSweep;

    public Gateway(Shortener shortener, Clock clock, Limits limits) {
        this.shortener = shortener;
        this.clock = clock;
        this.limits = limits;
        this.sweepEveryNanos = Math.max(
                limits.writeBurst() * limits.writeRefill().toNanos(),
                limits.readBurst() * limits.readRefill().toNanos());
        this.lastSweep = nanos();
    }

    public Response post(String client, String url, String alias) {
        long wait = admit(client, true);
        if (wait > 0) {
            return tooMany(wait);
        }
        try {
            String code = alias == null ? shortener.shorten(url) : shortener.shorten(url, alias);
            return new Response(201, code, Map.of("Location", "/" + code));
        } catch (IllegalArgumentException e) {
            return new Response(400, String.valueOf(e.getMessage()), Map.of());
        } catch (IllegalStateException e) {
            return new Response(409, String.valueOf(e.getMessage()), Map.of());
        }
    }

    public Response get(String client, String code) {
        long wait = admit(client, false);
        if (wait > 0) {
            return tooMany(wait);
        }
        return shortener.resolve(code)
                .map(url -> new Response(302, "", Map.of("Location", url)))
                .orElseGet(() -> new Response(404, "", Map.of()));
    }

    public synchronized int trackedClients() {
        return clients.size();
    }

    /** Takes a token for the client, or returns the nanoseconds until it may try again. */
    private synchronized long admit(String client, boolean write) {
        long now = nanos();
        if (now - lastSweep >= sweepEveryNanos) {
            clients.values().removeIf(c -> c.writes().isFull(now) && c.reads().isFull(now));
            lastSweep = now;
        }
        Client buckets = clients.computeIfAbsent(client, name -> new Client(
                new Bucket(limits.writeBurst(), limits.writeRefill(), now),
                new Bucket(limits.readBurst(), limits.readRefill(), now)));
        return (write ? buckets.writes() : buckets.reads()).take(now);
    }

    private static Response tooMany(long waitNanos) {
        long seconds = Math.max(1, (waitNanos + 999_999_999L) / 1_000_000_000L);
        return new Response(429, "", Map.of("Retry-After", Long.toString(seconds)));
    }

    private long nanos() {
        var instant = clock.instant();
        return instant.getEpochSecond() * 1_000_000_000L + instant.getNano();
    }
}
