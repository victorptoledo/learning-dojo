package dojo;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** An in-memory shard the tests can break and repair, counting the calls it gets. */
final class MemoryShard implements Shard {
    private final Map<String, String> data = new HashMap<>();
    boolean failing;
    boolean answersPing = true;
    int puts;
    int gets;
    int pings;

    @Override
    public void put(String key, String value) {
        puts++;
        if (failing) {
            throw new ShardUnavailableException("down");
        }
        data.put(key, value);
    }

    @Override
    public Optional<String> get(String key) {
        gets++;
        if (failing) {
            throw new ShardUnavailableException("down");
        }
        return Optional.ofNullable(data.get(key));
    }

    @Override
    public boolean ping() {
        pings++;
        return answersPing && !failing;
    }

    boolean holds(String key) {
        return data.containsKey(key);
    }
}
