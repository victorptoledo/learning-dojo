package dojo;

/** Thrown by the client when no replica of a key accepted a write. */
public class UnavailableException extends RuntimeException {
    public UnavailableException(String message) {
        super(message);
    }
}
