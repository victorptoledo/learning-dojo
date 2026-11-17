package dojo.rooms;

/** A request that clashes with the current data: 409. */
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}
