package dojo;

/** Thrown by the store when a write carries a token older than one it has already seen. */
public class StaleTokenException extends RuntimeException {
    public StaleTokenException(long token, long highest) {
        super("token " + token + " is older than " + highest);
    }
}
