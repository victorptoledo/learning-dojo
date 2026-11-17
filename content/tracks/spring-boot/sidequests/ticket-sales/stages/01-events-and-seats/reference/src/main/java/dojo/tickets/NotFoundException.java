package dojo.tickets;

/** Something that does not exist, or that belongs to someone else: 404. */
public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) {
        super(message);
    }
}
