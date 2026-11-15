package dojo;

import java.util.List;
import java.util.Set;

/** A consistent hash ring with virtual nodes. */
public final class HashRing {

    public HashRing(int virtualNodes) {
        throw new UnsupportedOperationException("todo ring with " + virtualNodes + " virtual nodes");
    }

    /** Places the node at its virtual points. Adding it twice changes nothing. */
    public void add(String node) {
        throw new UnsupportedOperationException("todo add " + node);
    }

    /** Removes the node's points; an unknown node is ignored. */
    public void remove(String node) {
        throw new UnsupportedOperationException("todo remove " + node);
    }

    /** The node names, sorted. */
    public Set<String> nodes() {
        throw new UnsupportedOperationException("todo nodes");
    }

    /** The first node at or after the key's point, wrapping around. */
    public String nodeFor(String key) {
        throw new UnsupportedOperationException("todo node for " + key);
    }

    /** The owner and the next distinct nodes clockwise: the replicas of the key. */
    public List<String> nodesFor(String key, int count) {
        throw new UnsupportedOperationException("todo " + count + " nodes for " + key);
    }
}
