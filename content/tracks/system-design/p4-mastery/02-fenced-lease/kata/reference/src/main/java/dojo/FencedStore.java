package dojo;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** Storage that rejects writes from holders whose token has been superseded. */
public final class FencedStore {
    private final Map<String, String> data = new HashMap<>();
    private long highest;

    public synchronized void write(String key, String value, long token) {
        if (token < highest) {
            throw new StaleTokenException(token, highest);
        }
        highest = token;
        data.put(key, value);
    }

    public synchronized Optional<String> read(String key) {
        return Optional.ofNullable(data.get(key));
    }

    public synchronized long highestToken() {
        return highest;
    }
}
