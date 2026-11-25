package dojo;

/** A failure worth retrying: a timeout, a dropped connection, a 503. */
public class TransientException extends RuntimeException {
    public TransientException(String message) {
        super(message);
    }
}
