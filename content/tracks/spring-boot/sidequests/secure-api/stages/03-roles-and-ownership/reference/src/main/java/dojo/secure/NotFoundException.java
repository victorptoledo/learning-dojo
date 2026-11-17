package dojo.secure;

/** Something that does not exist, or that the user may not know exists: 404. */
public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) {
        super(message);
    }
}
