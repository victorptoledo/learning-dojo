package dojo;

import java.util.List;
import java.util.Map;

/** Fixed partitions assigned to nodes, rebalanced with as few moves as possible. */
public final class PartitionMap {

    public PartitionMap(int partitions) {
        throw new UnsupportedOperationException("todo map of " + partitions + " partitions");
    }

    /** The partition of a key: it never changes. */
    public int partitionFor(String key) {
        throw new UnsupportedOperationException("todo partition for " + key);
    }

    /** Adds a node and returns the partitions moved to it. */
    public List<Move> addNode(String node) {
        throw new UnsupportedOperationException("todo add " + node);
    }

    /** Removes a node and returns where its partitions went. */
    public List<Move> removeNode(String node) {
        throw new UnsupportedOperationException("todo remove " + node);
    }

    public String ownerOf(int partition) {
        throw new UnsupportedOperationException("todo owner of " + partition);
    }

    public String nodeFor(String key) {
        throw new UnsupportedOperationException("todo node for " + key);
    }

    /** Every node, sorted, with its partitions in ascending order. */
    public Map<String, List<Integer>> assignment() {
        throw new UnsupportedOperationException("todo assignment");
    }
}
