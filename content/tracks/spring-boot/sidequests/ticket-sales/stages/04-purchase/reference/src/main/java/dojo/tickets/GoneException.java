package dojo.tickets;

/** Something that existed and is over for good: 410. */
public class GoneException extends RuntimeException {
    public GoneException(String message) {
        super(message);
    }
}
