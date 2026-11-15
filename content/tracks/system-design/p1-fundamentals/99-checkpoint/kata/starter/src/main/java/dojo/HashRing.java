package dojo;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/** A consistent hash ring with virtual nodes (the Hash Ring kata, solved). */
public final class HashRing {
    private final int virtualNodes;
    private final NavigableMap<Long, String> points = new TreeMap<>();
    private final Set<String> nodes = new TreeSet<>();

    public HashRing(int virtualNodes) {
        if (virtualNodes < 1) {
            throw new IllegalArgumentException("at least one virtual node per node");
        }
        this.virtualNodes = virtualNodes;
    }

    public void add(String node) {
        if (node == null || node.isBlank()) {
            throw new IllegalArgumentException("a node needs a name");
        }
        if (nodes.add(node)) {
            for (int i = 0; i < virtualNodes; i++) {
                points.put(Hashing.hash64(node + "#" + i), node);
            }
        }
    }

    public void remove(String node) {
        if (nodes.remove(node)) {
            for (int i = 0; i < virtualNodes; i++) {
                points.remove(Hashing.hash64(node + "#" + i), node);
            }
        }
    }

    public Set<String> nodes() {
        return Collections.unmodifiableSet(new TreeSet<>(nodes));
    }

    public String nodeFor(String key) {
        if (points.isEmpty()) {
            throw new IllegalStateException("the ring has no nodes");
        }
        Map.Entry<Long, String> entry = points.ceilingEntry(Hashing.hash64(key));
        return (entry != null ? entry : points.firstEntry()).getValue();
    }

    /** The owner and the next distinct nodes clockwise. */
    public List<String> nodesFor(String key, int count) {
        if (count < 0) {
            throw new IllegalArgumentException("count cannot be negative");
        }
        int wanted = Math.min(count, nodes.size());
        Set<String> found = new LinkedHashSet<>();
        long hash = Hashing.hash64(key);
        for (String node : points.tailMap(hash, true).values()) {
            if (found.size() == wanted) {
                break;
            }
            found.add(node);
        }
        for (String node : points.headMap(hash, false).values()) {
            if (found.size() == wanted) {
                break;
            }
            found.add(node);
        }
        return List.copyOf(found);
    }
}
