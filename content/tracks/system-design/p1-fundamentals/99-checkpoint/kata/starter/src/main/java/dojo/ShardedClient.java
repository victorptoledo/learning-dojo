package dojo;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Places keys on a ring of shards with replicas, health tracking and a Bloom filter per shard. */
public final class ShardedClient {

    public ShardedClient(Map<String, Shard> shards, int virtualNodes, int replicas, int failureThreshold) {
        throw new UnsupportedOperationException("todo client over " + shards.keySet());
    }

    /** Writes to the healthy replicas; returns how many acknowledged. */
    public int put(String key, String value) {
        throw new UnsupportedOperationException("todo put " + key);
    }

    /** The first value found on a healthy replica that may hold the key. */
    public Optional<String> get(String key) {
        throw new UnsupportedOperationException("todo get " + key);
    }

    /** The names of the healthy shards, sorted. */
    public Set<String> healthy() {
        throw new UnsupportedOperationException("todo healthy");
    }

    /** Pings the shards that are down and brings back those that answer. */
    public void probe() {
        throw new UnsupportedOperationException("todo probe");
    }

    /** Reads not sent to a shard because its Bloom filter ruled the key out. */
    public long skippedByFilter() {
        throw new UnsupportedOperationException("todo skipped");
    }
}
