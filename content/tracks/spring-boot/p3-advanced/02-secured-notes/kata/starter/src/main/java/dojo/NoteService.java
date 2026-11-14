package dojo;

import java.util.List;
import org.springframework.stereotype.Service;

/** Notes in memory. A user sees and deletes only their own notes; an admin sees all. */
@Service
public class NoteService {

    public Note create(String owner, String text) {
        throw new UnsupportedOperationException("todo create for " + owner);
    }

    public List<Note> ownedBy(String owner) {
        throw new UnsupportedOperationException("todo notes of " + owner);
    }

    /** The note if {@code user} owns it or is an admin; NoteNotFoundException otherwise. */
    public Note get(long id, String user, boolean admin) {
        throw new UnsupportedOperationException("todo get " + id);
    }

    public void delete(long id, String user, boolean admin) {
        throw new UnsupportedOperationException("todo delete " + id);
    }

    public List<Note> all() {
        throw new UnsupportedOperationException("todo all notes");
    }

    public int count() {
        throw new UnsupportedOperationException("todo count");
    }
}
