package dojo;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** One in-memory replica. It keeps the highest version it was sent for each key. */
public final class Replica {
    private final String name;
    private final Map<String, Versioned> data = new HashMap<>();
    private boolean up = true;
    private int reads;
    private int writes;

    public Replica(String name) {
        this.name = name;
    }

    public synchronized void write(String key, Versioned versioned) {
        if (!up) {
            throw new ReplicaDownException(name);
        }
        writes++;
        data.merge(key, versioned, (old, incoming) -> incoming.version() > old.version() ? incoming : old);
    }

    public synchronized Optional<Versioned> read(String key) {
        if (!up) {
            throw new ReplicaDownException(name);
        }
        reads++;
        return Optional.ofNullable(data.get(key));
    }

    /** What the replica holds, even while down (for tests and debugging). */
    public synchronized Optional<Versioned> stored(String key) {
        return Optional.ofNullable(data.get(key));
    }

    public synchronized void down() {
        up = false;
    }

    public synchronized void up() {
        up = true;
    }

    public synchronized int reads() {
        return reads;
    }

    public synchronized int writes() {
        return writes;
    }

    public String name() {
        return name;
    }
}
