package dojo.wallet;

/** Something that does not exist: 404. */
public class NotFoundException extends RuntimeException {
    public NotFoundException(String what, long id) {
        super(what + " " + id + " not found");
    }
}
