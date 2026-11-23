package dojo.cluster;

import java.time.Clock;

/** The client of a cache spread over many nodes. See README.md. */
public final class CacheCluster {

    public CacheCluster(Origin origin, Clock clock, ClusterConfig config) {
    }

    public void addNode(String name, CacheNode node, int weight) {
        throw new UnsupportedOperationException("todo: addNode");
    }

    public void removeNode(String name) {
        throw new UnsupportedOperationException("todo: removeNode");
    }

    public String ownerOf(String key) {
        throw new UnsupportedOperationException("todo: ownerOf");
    }

    public String get(String key) {
        throw new UnsupportedOperationException("todo: get");
    }

    public void invalidate(String key) {
        throw new UnsupportedOperationException("todo: invalidate");
    }
}
