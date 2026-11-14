package dojo;

/** Also thrown for notes of other users, so their ids are not revealed. */
public class NoteNotFoundException extends RuntimeException {
    public NoteNotFoundException(long id) {
        super("note " + id + " not found");
    }
}
