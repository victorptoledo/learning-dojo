package dojo;

import java.util.Optional;

/** One cache server. Every method may throw ShardUnavailableException. */
public interface Shard {
    void put(String key, String value);

    Optional<String> get(String key);

    /** The health check: true when the shard can serve again. */
    boolean ping();
}
