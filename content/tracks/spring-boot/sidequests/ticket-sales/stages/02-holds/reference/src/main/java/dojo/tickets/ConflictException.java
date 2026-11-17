package dojo.tickets;

/** A request the current state refuses: 409. */
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}
