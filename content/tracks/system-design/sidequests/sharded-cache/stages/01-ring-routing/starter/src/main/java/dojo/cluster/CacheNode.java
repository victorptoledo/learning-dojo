package dojo.cluster;

import java.util.Optional;
import java.util.Set;

/**
 * One cache server. Every call is a network round trip and may throw `NodeDownException`
 * (used from stage 4). Implementations are safe to call from many threads.
 */
public interface CacheNode {

    Optional<String> get(String key);

    void put(String key, String value);

    /** `false` when the key was not there. */
    boolean remove(String key);

    /** Every key on the node: a full scan, expensive on a real server. */
    Set<String> keys();
}
