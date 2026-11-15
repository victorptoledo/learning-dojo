package dojo;

/** Thrown by a shard that cannot serve (a timeout, a refused connection). */
public class ShardUnavailableException extends RuntimeException {
    public ShardUnavailableException(String message) {
        super(message);
    }
}
