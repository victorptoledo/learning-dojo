package dojo.cluster;

import java.time.Clock;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

/** The client of a cache spread over many nodes, with live resharding. See README.md. */
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
    }

    /** The state of a join: where keys used to live, and what is left to copy. */
    private static final class Migration {
        final Ring previous;
        final String target;
        final Deque<String> unscanned;
        final Deque<String[]> pending = new ArrayDeque<>(); // {node, key}
        final Set<String> moved = new HashSet<>();

        Migration(Ring previous, String target, Set<String> oldNodes) {
            this.previous = previous;
            this.target = target;
            this.unscanned = new ArrayDeque<>(oldNodes);
        }
    }

    private final Origin origin;
    private final ClusterConfig config;
    private final Map<String, CacheNode> nodes = new HashMap<>();
    private final Map<String, Integer> weights = new HashMap<>();
    private Ring ring;
    private Migration migration;

    public CacheCluster(Origin origin, Clock clock, ClusterConfig config) {
        this.origin = origin;
        this.config = config;
        this.ring = new Ring(weights, config.virtualNodes());
    }

    public void addNode(String name, CacheNode node, int weight) {
        checkNew(name, weight);
        nodes.put(name, node);
        weights.put(name, weight);
        ring = new Ring(weights, config.virtualNodes());
    }

    public void joinNode(String name, CacheNode node, int weight) {
        checkNew(name, weight);
        Ring previous = ring;
        Set<String> oldNodes = new HashSet<>(nodes.keySet());
        addNode(name, node, weight);
        if (!oldNodes.isEmpty()) {
            migration = new Migration(previous, name, oldNodes);
        }
    }

    public void removeNode(String name) {
        checkStable();
        if (!nodes.containsKey(name)) {
            throw new IllegalArgumentException("no such node: " + name);
        }
        nodes.remove(name);
        weights.remove(name);
        ring = new Ring(weights, config.virtualNodes());
    }

    public String ownerOf(String key) {
        if (ring.isEmpty()) {
            throw new IllegalStateException("the cluster has no nodes");
        }
        return ring.owner(key);
    }

    public String get(String key) {
        String ownerName = ownerOf(key);
        CacheNode owner = nodes.get(ownerName);
        Optional<String> cached = owner.get(key);
        if (cached.isPresent()) {
            return cached.get();
        }
        if (migration != null && ownerName.equals(migration.target)) {
            CacheNode previous = nodes.get(migration.previous.owner(key));
            Optional<String> old = previous.get(key);
            if (old.isPresent()) {
                owner.put(key, old.get());
                previous.remove(key);
                migration.moved.add(key);
                return old.get();
            }
        }
        String value = origin.load(key);
        owner.put(key, value);
        return value;
    }

    public void invalidate(String key) {
        if (ring.isEmpty()) {
            return;
        }
        String ownerName = ownerOf(key);
        nodes.get(ownerName).remove(key);
        if (migration != null && ownerName.equals(migration.target)) {
            nodes.get(migration.previous.owner(key)).remove(key);
        }
    }

    public int migrateStep(int maxKeys) {
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

    public boolean migrating() {
        return migration != null;
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
