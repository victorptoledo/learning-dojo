package dojo;

import java.util.List;
import java.util.Optional;

/**
 * A partitioned key-value store: one leader per partition, asynchronous followers that consume
 * the leader's log, session guarantees on reads and failover that knows what it lost.
 */
public final class ReplicatedStore {

    /** Partition i is served by the nodes of list i; the first one leads epoch 1. */
    public ReplicatedStore(List<List<Node>> partitions) {
        throw new UnsupportedOperationException("todo a store with " + partitions.size() + " partitions");
    }

    public int partitionFor(String key) {
        throw new UnsupportedOperationException("todo the partition of " + key);
    }

    public String leaderOf(int partition) {
        throw new UnsupportedOperationException("todo the leader of " + partition);
    }

    public long epochOf(int partition) {
        throw new UnsupportedOperationException("todo the epoch of " + partition);
    }

    public Session session() {
        throw new UnsupportedOperationException("todo a new session");
    }

    /** Writes on the partition's leader and returns the position of the write. */
    public long put(Session session, String key, String value) {
        throw new UnsupportedOperationException("todo put " + key);
    }

    /** Reads from a follower that has everything the session has seen, or else from the leader. */
    public Optional<String> get(Session session, String key) {
        throw new UnsupportedOperationException("todo get " + key);
    }

    /** Ships up to maxEntries log entries from each leader to each of its followers. */
    public int replicate(int maxEntries) {
        throw new UnsupportedOperationException("todo replicate " + maxEntries);
    }

    /** How many entries a node is behind its partition's leader. */
    public long lag(String node) {
        throw new UnsupportedOperationException("todo the lag of " + node);
    }

    /** Promotes the most advanced follower of a partition whose leader is down. */
    public Failover failover(int partition) {
        throw new UnsupportedOperationException("todo failover of " + partition);
    }

    /** Entries dropped from nodes that came back with writes a failover had lost. */
    public long truncatedEntries() {
        throw new UnsupportedOperationException("todo truncated entries");
    }
}
