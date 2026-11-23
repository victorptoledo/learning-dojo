package dojo.cluster;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/** Test doubles: a clock moved by hand, nodes and a database that count calls, and the ring computed apart. */
final class Fakes {
    private Fakes() {}

    /** Every setting off except the ring: the stages override what they test. */
    static ClusterConfig config() {
        return new ClusterConfig(100, Duration.ofSeconds(1), Integer.MAX_VALUE, 1, Integer.MAX_VALUE,
                Duration.ofSeconds(30), Integer.MAX_VALUE);
    }

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

    static final class FakeNode implements CacheNode {
        final String name;
        final ConcurrentHashMap<String, String> data = new ConcurrentHashMap<>();
        final AtomicInteger gets = new AtomicInteger();
        final AtomicInteger puts = new AtomicInteger();
        final AtomicInteger removes = new AtomicInteger();
        final AtomicInteger scans = new AtomicInteger();
        final AtomicInteger calls = new AtomicInteger();
        volatile boolean down;

        FakeNode(String name) {
            this.name = name;
        }

        private void call() {
            calls.incrementAndGet();
            if (down) {
                throw new NodeDownException(name + " is down");
            }
        }

        @Override
        public Optional<String> get(String key) {
            gets.incrementAndGet();
            call();
            return Optional.ofNullable(data.get(key));
        }

        @Override
        public void put(String key, String value) {
            puts.incrementAndGet();
            call();
            data.put(key, value);
        }

        @Override
        public boolean remove(String key) {
            removes.incrementAndGet();
            call();
            return data.remove(key) != null;
        }

        @Override
        public Set<String> keys() {
            scans.incrementAndGet();
            call();
            return new HashSet<>(data.keySet());
        }

        void resetCounts() {
            gets.set(0);
            puts.set(0);
            removes.set(0);
            scans.set(0);
            calls.set(0);
        }
    }

    static final class FakeOrigin implements Origin {
        final ConcurrentHashMap<String, String> values = new ConcurrentHashMap<>();
        final ConcurrentHashMap<String, AtomicInteger> loadsByKey = new ConcurrentHashMap<>();
        final AtomicInteger loads = new AtomicInteger();
        final AtomicInteger running = new AtomicInteger();
        final AtomicInteger maxRunning = new AtomicInteger();
        final Set<String> failing = ConcurrentHashMap.newKeySet();
        /** When set, loads of these keys wait for the latch: a slow database. */
        final Map<String, CountDownLatch> gates = new ConcurrentHashMap<>();
        /** Counts down when a gated load starts waiting. */
        final Map<String, CountDownLatch> arrived = new ConcurrentHashMap<>();

        @Override
        public String load(String key) {
            loads.incrementAndGet();
            loadsByKey.computeIfAbsent(key, k -> new AtomicInteger()).incrementAndGet();
            int now = running.incrementAndGet();
            maxRunning.accumulateAndGet(now, Math::max);
            try {
                CountDownLatch gate = gates.get(key);
                if (gate != null) {
                    CountDownLatch here = arrived.get(key);
                    if (here != null) {
                        here.countDown();
                    }
                    if (!gate.await(20, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("the test never opened the gate of " + key);
                    }
                }
                if (failing.contains(key)) {
                    throw new IllegalStateException("database error for " + key);
                }
                return values.getOrDefault(key, "value-of-" + key);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(e);
            } finally {
                running.decrementAndGet();
            }
        }

        int loadsOf(String key) {
            AtomicInteger n = loadsByKey.get(key);
            return n == null ? 0 : n.get();
        }

        /** Makes loads of the key wait; returns the latch that says a load is waiting. */
        CountDownLatch gate(String key, CountDownLatch gate) {
            CountDownLatch here = new CountDownLatch(1);
            arrived.put(key, here);
            gates.put(key, gate);
            return here;
        }
    }

    /** The ring of the README, computed apart from the learner's code. */
    static final class TestRing {
        private final TreeMap<Long, String> points = new TreeMap<>();

        TestRing(int virtualNodes, Map<String, Integer> weights) {
            for (var node : weights.entrySet()) {
                for (int i = 0; i < node.getValue() * virtualNodes; i++) {
                    long at = Hashing.hash64(node.getKey() + "#" + i);
                    points.merge(at, node.getKey(), (a, b) -> a.compareTo(b) <= 0 ? a : b);
                }
            }
        }

        String owner(String key) {
            var at = points.ceilingEntry(Hashing.hash64(key));
            return (at == null ? points.firstEntry() : at).getValue();
        }

        /** The owner and the next distinct nodes clockwise. */
        List<String> walk(String key, int count) {
            List<String> nodes = new java.util.ArrayList<>();
            var tail = points.tailMap(Hashing.hash64(key), true).values();
            for (Iterable<String> part : List.of(tail, points.values())) {
                for (String node : part) {
                    if (!nodes.contains(node)) {
                        nodes.add(node);
                    }
                    if (nodes.size() == count) {
                        return nodes;
                    }
                }
            }
            return nodes;
        }
    }

    static List<String> keys(int count) {
        List<String> keys = new java.util.ArrayList<>();
        for (int i = 0; i < count; i++) {
            keys.add("user:" + i);
        }
        return keys;
    }
}
