package dojo.shortener;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/** The test doubles: a clock moved by hand, a store and a ticket server that count round trips. */
final class Fakes {
    private Fakes() {}

    static final class ManualClock extends Clock {
        private volatile Instant now = Instant.parse("2026-11-23T12:00:00Z");

        void advance(Duration by) {
            now = now.plus(by);
        }

        @Override
        public Instant instant() {
            return now;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }
    }

    static final class MemoryStore implements LinkStore {
        final ConcurrentHashMap<String, String> links = new ConcurrentHashMap<>();
        final AtomicInteger puts = new AtomicInteger();
        final AtomicInteger gets = new AtomicInteger();
        final AtomicInteger deletes = new AtomicInteger();

        @Override
        public boolean putIfAbsent(String code, String url) {
            puts.incrementAndGet();
            return links.putIfAbsent(code, url) == null;
        }

        @Override
        public Optional<String> get(String code) {
            gets.incrementAndGet();
            return Optional.ofNullable(links.get(code));
        }

        @Override
        public boolean delete(String code) {
            deletes.incrementAndGet();
            return links.remove(code) != null;
        }
    }

    static final class MemoryAllocator implements IdAllocator {
        private final AtomicLong next;
        final AtomicInteger reservations = new AtomicInteger();

        MemoryAllocator(long first) {
            next = new AtomicLong(first);
        }

        @Override
        public long reserve(int count) {
            if (count <= 0) {
                throw new IllegalArgumentException("count must be positive: " + count);
            }
            reservations.incrementAndGet();
            return next.getAndAdd(count);
        }
    }

    static ShortenerConfig config(int blockSize, int codeLength) {
        return new ShortenerConfig(blockSize, codeLength, 1000, Duration.ofMinutes(10), Duration.ofMinutes(1));
    }
}
