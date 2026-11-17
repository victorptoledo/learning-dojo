package dojo.secure;

/** A request the current data refuses: 409. */
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}
