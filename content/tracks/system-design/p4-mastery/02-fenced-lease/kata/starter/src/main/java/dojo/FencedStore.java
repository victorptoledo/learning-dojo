package dojo;

import java.util.Optional;

/** Storage that rejects writes from holders whose token has been superseded. */
public final class FencedStore {

    public void write(String key, String value, long token) {
        throw new UnsupportedOperationException("todo write " + key + " with token " + token);
    }

    public Optional<String> read(String key) {
        throw new UnsupportedOperationException("todo read " + key);
    }

    public long highestToken() {
        throw new UnsupportedOperationException("todo highest token");
    }
}
