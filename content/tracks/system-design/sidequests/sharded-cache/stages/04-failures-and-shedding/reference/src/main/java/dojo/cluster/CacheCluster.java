package dojo.cluster;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * The client of a cache spread over many nodes: resharding, hot keys, coalesced misses,
 * ejection of failing nodes and load shedding in front of the database. See README.md.
 */
public final class CacheCluster {

    /** A ring built from node weights, by the rules of the README. */
    private static final class Ring {
        private final TreeMap<Long, String> points = new TreeMap<>();

        Ring(Map<String, Integer> weights, int virtualNodes) {
            for (var node : weights.entrySet()) {
                for (int i = 0; i < node.getValue() * virtualNodes; i++) {
                    points.merge(Hashing.hash64(node.getKey() + "#" + i), node.getKey(),
                            (a, b) -> a.compareTo(b) <= 0 ? a : b);
                }
            }
        }

        boolean isEmpty() {
            return points.isEmpty();
        }

        String owner(String key) {
            var at = points.ceilingEntry(Hashing.hash64(key));
            return (at == null ? points.firstEntry() : at).getValue();
        }

        /** The first `count` distinct nodes clockwise from the key that pass the filter. */
        List<String> walk(String key, int count, Predicate<String> usable) {
            List<String> nodes = new ArrayList<>();
            var tail = points.tailMap(Hashing.hash64(key), true).values();
            for (Iterable<String> part : List.of(tail, points.values())) {
                for (String node : part) {
                    if (nodes.size() == count) {
                        return nodes;
                    }
                    if (usable.test(node) && !nodes.contains(node)) {
                        nodes.add(node);
                    }
                }
            }
            return nodes;
        }
    }

    /** The state of a join: where keys used to live, and what is left to copy. */
    private static final class Migration {
        final Ring previous;
        final String target;
        final Deque<String> unscanned;
        final Deque<String[]> pending = new ArrayDeque<>(); // {node, key}
        final Set<String> moved = java.util.concurrent.ConcurrentHashMap.newKeySet();

        Migration(Ring previous, String target, Set<String> oldNodes) {
            this.previous = previous;
            this.target = target;
            this.unscanned = new ArrayDeque<>(oldNodes);
        }
    }

    /** A hot key: hot through window `until`; `next` picks the copy of the next read. */
    private static final class Hot {
        long until;
        long next;
    }

    /** Failures in a row, and the end of an ejection (null when the node is in). */
    private static final class Health {
        int failures;
        Instant ejectedUntil;
    }

    private final Origin origin;
    private final Clock clock;
    private final ClusterConfig config;
    private final Map<String, CacheNode> nodes = new HashMap<>();
    private final Map<String, Integer> weights = new HashMap<>();
    private final Map<String, Health> health = new HashMap<>();
    private Ring ring;
    private Migration migration;

    // Hot key bookkeeping (guarded by this): read counts of the current window only.
    private long window = Long.MIN_VALUE;
    private Map<String, Integer> counts = new HashMap<>();
    private final Map<String, Hot> hot = new HashMap<>();
    private final Set<String> replicated = new HashSet<>();

    // Database loads (guarded by `loading`): one per key, at most originConcurrency at once.
    private final Map<String, CompletableFuture<String>> loading = new HashMap<>();

    public CacheCluster(Origin origin, Clock clock, ClusterConfig config) {
        this.origin = origin;
        this.clock = clock;
        this.config = config;
        this.ring = new Ring(weights, config.virtualNodes());
    }

    public synchronized void addNode(String name, CacheNode node, int weight) {
        checkNew(name, weight);
        nodes.put(name, node);
        weights.put(name, weight);
        health.put(name, new Health());
        ring = new Ring(weights, config.virtualNodes());
    }

    public synchronized void joinNode(String name, CacheNode node, int weight) {
        checkNew(name, weight);
        Ring previous = ring;
        Set<String> oldNodes = new HashSet<>(nodes.keySet());
        addNode(name, node, weight);
        if (!oldNodes.isEmpty()) {
            migration = new Migration(previous, name, oldNodes);
        }
    }

    public synchronized void removeNode(String name) {
        checkStable();
        if (!nodes.containsKey(name)) {
            throw new IllegalArgumentException("no such node: " + name);
        }
        nodes.remove(name);
        weights.remove(name);
        health.remove(name);
        ring = new Ring(weights, config.virtualNodes());
    }

    public synchronized String ownerOf(String key) {
        if (ring.isEmpty()) {
            throw new IllegalStateException("the cluster has no nodes");
        }
        List<String> usable = ring.walk(key, 1, this::usable);
        return usable.isEmpty() ? ring.owner(key) : usable.get(0);
    }

    public String get(String key) {
        String ownerName;
        String copyName = null;
        List<String> replicas = List.of();
        Migration moving;
        synchronized (this) {
            ownerName = ownerOf(key);
            moving = migration;
            long now = windowOf(clock.instant());
            roll(now);
            Hot state = hot.get(key);
            boolean wasHot = state != null && state.until >= now;
            if (state != null && !wasHot) {
                hot.remove(key);
                state = null;
            }
            if (wasHot) {
                List<String> copies = ring.walk(key, config.hotReplicas(), this::usable);
                String chosen = copies.get((int) (state.next++ % copies.size()));
                if (!chosen.equals(ownerName)) {
                    copyName = chosen;
                }
            }
            int reads = counts.merge(key, 1, Integer::sum);
            if (reads >= config.hotThreshold()) {
                if (state == null) {
                    state = new Hot();
                    hot.put(key, state);
                    replicas = new ArrayList<>(ring.walk(key, config.hotReplicas(), this::usable));
                    replicas.remove(ownerName);
                    replicated.add(key);
                }
                state.until = Math.max(state.until, now + 1);
            }
        }
        if (copyName != null) {
            String copy = copyName;
            Optional<Optional<String>> answer = call(copy, () -> nodes(copy).get(key));
            if (answer.isPresent() && answer.get().isPresent()) {
                return answer.get().get();
            }
            String refilled = readOwner(key, ownerName, moving);
            if (answer.isPresent()) {
                call(copy, () -> {
                    nodes(copy).put(key, refilled);
                    return true;
                });
            }
            return refilled;
        }
        String value = readOwner(key, ownerName, moving);
        for (String replica : replicas) {
            call(replica, () -> {
                nodes(replica).put(key, value);
                return true;
            });
        }
        return value;
    }

    public void invalidate(String key) {
        List<String> targets = new ArrayList<>();
        synchronized (this) {
            if (ring.isEmpty()) {
                return;
            }
            String ownerName = ownerOf(key);
            targets.add(ownerName);
            if (migration != null && ownerName.equals(migration.target)) {
                targets.add(migration.previous.owner(key));
            }
            if (replicated.remove(key)) {
                for (String name : ring.walk(key, config.hotReplicas(), n -> true)) {
                    if (!targets.contains(name)) {
                        targets.add(name);
                    }
                }
            }
        }
        for (String name : targets) {
            call(name, () -> nodes(name).remove(key));
        }
    }

    public synchronized int migrateStep(int maxKeys) {
        if (migration == null) {
            return 0;
        }
        int moved = 0;
        while (moved < maxKeys) {
            if (migration.pending.isEmpty()) {
                if (migration.unscanned.isEmpty()) {
                    migration = null;
                    break;
                }
                String scanned = migration.unscanned.poll();
                for (String key : call(scanned, () -> nodes.get(scanned).keys()).orElse(Set.of())) {
                    if (ring.owner(key).equals(migration.target)) {
                        migration.pending.add(new String[] {scanned, key});
                    }
                }
                continue;
            }
            String[] next = migration.pending.poll();
            if (!migration.moved.add(next[1])) {
                continue;
            }
            Optional<String> value = call(next[0], () -> nodes.get(next[0]).get(next[1])).flatMap(v -> v);
            if (value.isPresent()) {
                String target = migration.target;
                if (call(target, () -> {
                    nodes.get(target).put(next[1], value.get());
                    return true;
                }).isPresent()) {
                    call(next[0], () -> nodes.get(next[0]).remove(next[1]));
                    moved++;
                }
            }
        }
        return moved;
    }

    public synchronized boolean migrating() {
        return migration != null;
    }

    /** The owner's value, else the previous owner's during a migration, else the database. */
    private String readOwner(String key, String ownerName, Migration moving) {
        String previous = null;
        if (moving != null && ownerName.equals(moving.target)) {
            previous = moving.previous.owner(key);
        }
        Optional<Optional<String>> cached = call(ownerName, () -> nodes(ownerName).get(key));
        if (cached.isPresent() && cached.get().isPresent()) {
            return cached.get().get();
        }
        boolean ownerUp = cached.isPresent();
        if (previous != null && ownerUp) {
            String from = previous;
            Optional<String> old = call(from, () -> nodes(from).get(key)).flatMap(v -> v);
            if (old.isPresent()) {
                call(ownerName, () -> {
                    nodes(ownerName).put(key, old.get());
                    return true;
                });
                call(from, () -> nodes(from).remove(key));
                moving.moved.add(key);
                return old.get();
            }
        }
        return load(key, ownerUp ? ownerName : null);
    }

    /** One database load per key, at most originConcurrency at once; the value is stored before waiters go. */
    private String load(String key, String ownerName) {
        var mine = new CompletableFuture<String>();
        CompletableFuture<String> running;
        synchronized (loading) {
            running = loading.get(key);
            if (running == null) {
                if (loading.size() >= config.originConcurrency()) {
                    throw new OverloadedException("the database already has " + loading.size() + " loads running");
                }
                loading.put(key, mine);
            }
        }
        if (running != null) {
            try {
                return running.join();
            } catch (CompletionException e) {
                throw e.getCause() instanceof RuntimeException r ? r : e;
            }
        }
        try {
            String value = origin.load(key);
            if (ownerName != null) {
                call(ownerName, () -> {
                    nodes(ownerName).put(key, value);
                    return true;
                });
            }
            mine.complete(value);
            return value;
        } catch (RuntimeException e) {
            mine.completeExceptionally(e);
            throw e;
        } finally {
            synchronized (loading) {
                loading.remove(key, mine);
            }
        }
    }

    /** Calls a node; a `NodeDownException` counts a failure and gives empty. */
    private <T> Optional<T> call(String name, Supplier<T> action) {
        try {
            T result = action.get();
            synchronized (this) {
                Health h = health.get(name);
                if (h != null) {
                    h.failures = 0;
                    h.ejectedUntil = null;
                }
            }
            return Optional.ofNullable(result);
        } catch (NodeDownException e) {
            synchronized (this) {
                Health h = health.get(name);
                if (h != null && (h.ejectedUntil != null || ++h.failures >= config.failureThreshold())) {
                    h.ejectedUntil = clock.instant().plus(config.ejectFor());
                    h.failures = 0;
                }
            }
            return Optional.empty();
        }
    }

    private synchronized CacheNode nodes(String name) {
        return nodes.get(name);
    }

    /** In the cluster, or ejected long enough ago that the next call is a probe. */
    private boolean usable(String name) {
        Health h = health.get(name);
        return h == null || h.ejectedUntil == null || !clock.instant().isBefore(h.ejectedUntil);
    }

    private long windowOf(Instant instant) {
        long nanos = instant.getEpochSecond() * 1_000_000_000L + instant.getNano();
        return Math.floorDiv(nanos, config.hotWindow().toNanos());
    }

    /** Starts a new counting window when the clock moved past the current one. */
    private void roll(long now) {
        if (now != window) {
            counts = new HashMap<>();
            window = now;
            hot.values().removeIf(h -> h.until < now);
        }
    }

    private void checkNew(String name, int weight) {
        checkStable();
        if (name == null || name.isBlank() || nodes.containsKey(name)) {
            throw new IllegalArgumentException("a node needs a new, non-blank name: " + name);
        }
        if (weight < 1) {
            throw new IllegalArgumentException("a weight is at least 1: " + weight);
        }
    }

    private void checkStable() {
        if (migration != null) {
            throw new IllegalStateException("a migration is running: finish it first");
        }
    }
}
