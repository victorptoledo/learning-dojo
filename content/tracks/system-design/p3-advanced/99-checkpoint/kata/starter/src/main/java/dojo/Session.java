package dojo;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** A client's session: one token per partition it has written to or read from. */
public final class Session {
    private final Map<Integer, Token> tokens = new HashMap<>();

    /** The token of a partition, or empty if the session has not touched it yet. */
    public Optional<Token> token(int partition) {
        return Optional.ofNullable(tokens.get(partition));
    }

    /** Replaces the token of a partition. The store decides what the new token is. */
    public void advance(int partition, Token token) {
        tokens.put(partition, token);
    }
}
