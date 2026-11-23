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
import java.util.concurrent.ConcurrentHashMap;

/** The client of a cache spread over many nodes: resharding, hot keys, coalesced misses. See README.md. */
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

        /** The owner and the next distinct nodes clockwise, at most `count`. */
        List<String> walk(String key, int count) {
            List<String> nodes = new ArrayList<>();
            var tail = points.tailMap(Hashing.hash64(key), true).values();
            for (Iterable<String> part : List.of(tail, points.values())) {
                for (String node : part) {
                    if (nodes.size() == count) {
                        return nodes;
                    }
                    if (!nodes.contains(node)) {
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
        final Set<String> moved = ConcurrentHashMap.newKeySet();

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

    private final Origin origin;
    private final Clock clock;
    private final ClusterConfig config;
    private final Map<String, CacheNode> nodes = new HashMap<>();
    private final Map<String, Integer> weights = new HashMap<>();
    private Ring ring;
    private Migration migration;

    // Hot key bookkeeping (guarded by this): read counts of the current and previous windows.
    private long window = Long.MIN_VALUE;
    private Map<String, Integer> counts = new HashMap<>();
    private final Map<String, Hot> hot = new HashMap<>();
    private final Set<String> replicated = new HashSet<>();

    private final ConcurrentHashMap<String, CompletableFuture<String>> loading = new ConcurrentHashMap<>();

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
        ring = new Ring(weights, config.virtualNodes());
    }

    public synchronized String ownerOf(String key) {
        if (ring.isEmpty()) {
            throw new IllegalStateException("the cluster has no nodes");
        }
        return ring.owner(key);
    }

    public String get(String key) {
        String ownerName;
        CacheNode copy = null;
        List<CacheNode> replicas = List.of();
        boolean becameHot;
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
            List<String> copies = wasHot ? ring.walk(key, config.hotReplicas()) : List.of();
            if (wasHot) {
                String chosen = copies.get((int) (state.next++ % copies.size()));
                if (!chosen.equals(ownerName)) {
                    copy = nodes.get(chosen);
                }
            }
            int reads = counts.merge(key, 1, Integer::sum);
            becameHot = false;
            if (reads >= config.hotThreshold()) {
                if (state == null) {
                    state = new Hot();
                    hot.put(key, state);
                    becameHot = true;
                }
                state.until = Math.max(state.until, now + 1);
            }
            if (becameHot) {
                replicas = new ArrayList<>();
                for (String name : ring.walk(key, config.hotReplicas())) {
                    if (!name.equals(ownerName)) {
                        replicas.add(nodes.get(name));
                    }
                }
                replicated.add(key);
            }
        }
        if (copy != null) {
            Optional<String> value = copy.get(key);
            if (value.isPresent()) {
                return value.get();
            }
            String refilled = readOwner(key, ownerName, moving);
            copy.put(key, refilled);
            return refilled;
        }
        String value = readOwner(key, ownerName, moving);
        for (CacheNode replica : replicas) {
            replica.put(key, value);
        }
        return value;
    }

    public void invalidate(String key) {
        List<CacheNode> targets = new ArrayList<>();
        synchronized (this) {
            if (ring.isEmpty()) {
                return;
            }
            String ownerName = ring.owner(key);
            targets.add(nodes.get(ownerName));
            if (migration != null && ownerName.equals(migration.target)) {
                targets.add(nodes.get(migration.previous.owner(key)));
            }
            if (replicated.remove(key)) {
                for (String name : ring.walk(key, config.hotReplicas())) {
                    if (!name.equals(ownerName)) {
                        targets.add(nodes.get(name));
                    }
                }
            }
        }
        for (CacheNode node : targets) {
            node.remove(key);
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
                for (String key : nodes.get(scanned).keys()) {
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
            CacheNode from = nodes.get(next[0]);
            Optional<String> value = from.get(next[1]);
            if (value.isPresent()) {
                nodes.get(migration.target).put(next[1], value.get());
                from.remove(next[1]);
                moved++;
            }
        }
        return moved;
    }

    public synchronized boolean migrating() {
        return migration != null;
    }

    /** The owner's value, else the previous owner's during a migration, else the database. */
    private String readOwner(String key, String ownerName, Migration moving) {
        CacheNode owner;
        CacheNode previous = null;
        synchronized (this) {
            owner = nodes.get(ownerName);
            if (moving != null && ownerName.equals(moving.target)) {
                previous = nodes.get(moving.previous.owner(key));
            }
        }
        Optional<String> cached = owner.get(key);
        if (cached.isPresent()) {
            return cached.get();
        }
        if (previous != null) {
            Optional<String> old = previous.get(key);
            if (old.isPresent()) {
                owner.put(key, old.get());
                previous.remove(key);
                moving.moved.add(key);
                return old.get();
            }
        }
        return load(key, owner);
    }

    /** One database load per key at a time; the value is on the owner before waiters are let go. */
    private String load(String key, CacheNode owner) {
        var mine = new CompletableFuture<String>();
        var running = loading.putIfAbsent(key, mine);
        if (running != null) {
            try {
                return running.join();
            } catch (CompletionException e) {
                throw e.getCause() instanceof RuntimeException r ? r : e;
            }
        }
        try {
            String value = origin.load(key);
            owner.put(key, value);
            mine.complete(value);
            return value;
        } catch (RuntimeException e) {
            mine.completeExceptionally(e);
            throw e;
        } finally {
            loading.remove(key, mine);
        }
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
