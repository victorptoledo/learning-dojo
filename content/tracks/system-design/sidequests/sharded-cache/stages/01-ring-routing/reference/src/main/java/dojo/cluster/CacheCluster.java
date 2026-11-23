package dojo.cluster;

import java.time.Clock;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/** The client of a cache spread over many nodes. See README.md. */
public final class CacheCluster {
    private final Origin origin;
    private final ClusterConfig config;
    private final Map<String, CacheNode> nodes = new HashMap<>();
    private final Map<String, Integer> weights = new HashMap<>();
    private final TreeMap<Long, String> ring = new TreeMap<>();

    public CacheCluster(Origin origin, Clock clock, ClusterConfig config) {
        this.origin = origin;
        this.config = config;
    }

    public void addNode(String name, CacheNode node, int weight) {
        if (name == null || name.isBlank() || nodes.containsKey(name)) {
            throw new IllegalArgumentException("a node needs a new, non-blank name: " + name);
        }
        if (weight < 1) {
            throw new IllegalArgumentException("a weight is at least 1: " + weight);
        }
        nodes.put(name, node);
        weights.put(name, weight);
        rebuild();
    }

    public void removeNode(String name) {
        if (!nodes.containsKey(name)) {
            throw new IllegalArgumentException("no such node: " + name);
        }
        nodes.remove(name);
        weights.remove(name);
        rebuild();
    }

    public String ownerOf(String key) {
        if (ring.isEmpty()) {
            throw new IllegalStateException("the cluster has no nodes");
        }
        var at = ring.ceilingEntry(Hashing.hash64(key));
        return (at == null ? ring.firstEntry() : at).getValue();
    }

    public String get(String key) {
        CacheNode owner = nodes.get(ownerOf(key));
        Optional<String> cached = owner.get(key);
        if (cached.isPresent()) {
            return cached.get();
        }
        String value = origin.load(key);
        owner.put(key, value);
        return value;
    }

    public void invalidate(String key) {
        if (!ring.isEmpty()) {
            nodes.get(ownerOf(key)).remove(key);
        }
    }

    private void rebuild() {
        ring.clear();
        for (var node : weights.entrySet()) {
            for (int i = 0; i < node.getValue() * config.virtualNodes(); i++) {
                ring.merge(Hashing.hash64(node.getKey() + "#" + i), node.getKey(),
                        (a, b) -> a.compareTo(b) <= 0 ? a : b);
            }
        }
    }
}
