package dojo;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/** Places keys on a ring of shards with replicas, health tracking and a Bloom filter per shard. */
public final class ShardedClient {
    private static final class ShardState {
        final Shard shard;
        final BloomFilter filter = BloomFilter.forCapacity(100_000, 0.01);
        int failures;
        boolean down;

        ShardState(Shard shard) {
            this.shard = shard;
        }
    }

    private final Map<String, ShardState> states = new TreeMap<>();
    private final HashRing ring;
    private final int replicas;
    private final int failureThreshold;
    private long skipped;

    public ShardedClient(Map<String, Shard> shards, int virtualNodes, int replicas, int failureThreshold) {
        if (shards.isEmpty() || replicas < 1 || failureThreshold < 1) {
            throw new IllegalArgumentException("need shards, at least one replica and a threshold of at least 1");
        }
        this.ring = new HashRing(virtualNodes);
        this.replicas = replicas;
        this.failureThreshold = failureThreshold;
        shards.forEach((name, shard) -> {
            states.put(name, new ShardState(shard));
            ring.add(name);
        });
    }

    /** Writes to the healthy replicas; returns how many acknowledged. */
    public int put(String key, String value) {
        int acks = 0;
        for (String name : ring.nodesFor(key, replicas)) {
            ShardState state = states.get(name);
            if (state.down) {
                continue;
            }
            try {
                state.shard.put(key, value);
                state.failures = 0;
                state.filter.add(key);
                acks++;
            } catch (ShardUnavailableException e) {
                failed(state);
            }
        }
        if (acks == 0) {
            throw new UnavailableException("no replica of " + key + " accepted the write");
        }
        return acks;
    }

    /** The first value found on a healthy replica that may hold the key. */
    public Optional<String> get(String key) {
        for (String name : ring.nodesFor(key, replicas)) {
            ShardState state = states.get(name);
            if (state.down) {
                continue;
            }
            if (!state.filter.mightContain(key)) {
                skipped++;
                continue;
            }
            try {
                Optional<String> value = state.shard.get(key);
                state.failures = 0;
                if (value.isPresent()) {
                    return value;
                }
            } catch (ShardUnavailableException e) {
                failed(state);
            }
        }
        return Optional.empty();
    }

    /** The names of the healthy shards, sorted. */
    public Set<String> healthy() {
        Set<String> names = new TreeSet<>();
        states.forEach((name, state) -> {
            if (!state.down) {
                names.add(name);
            }
        });
        return names;
    }

    /** Pings the shards that are down and brings back those that answer. */
    public void probe() {
        for (ShardState state : states.values()) {
            if (state.down && state.shard.ping()) {
                state.down = false;
                state.failures = 0;
            }
        }
    }

    /** Reads not sent to a shard because its Bloom filter ruled the key out. */
    public long skippedByFilter() {
        return skipped;
    }

    private void failed(ShardState state) {
        state.failures++;
        if (state.failures >= failureThreshold) {
            state.down = true;
        }
    }
}
