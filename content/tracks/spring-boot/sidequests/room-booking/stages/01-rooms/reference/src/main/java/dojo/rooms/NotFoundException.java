package dojo.rooms;

/** A room or booking that does not exist: 404. */
public class NotFoundException extends RuntimeException {
    public NotFoundException(String what, long id) {
        super(what + " " + id + " not found");
    }
}
