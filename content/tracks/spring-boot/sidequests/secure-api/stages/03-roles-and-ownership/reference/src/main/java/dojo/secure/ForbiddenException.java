package dojo.secure;

/** Something the user sees but may not change: 403. */
public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
        super(message);
    }
}
