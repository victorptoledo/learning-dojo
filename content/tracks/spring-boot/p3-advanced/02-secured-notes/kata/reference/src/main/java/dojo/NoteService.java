package dojo;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Service;

/** Notes in memory. A user sees and deletes only their own notes; an admin sees all. */
@Service
public class NoteService {
    private final Map<Long, Note> notes = new ConcurrentSkipListMap<>();
    private final AtomicLong lastId = new AtomicLong();

    public Note create(String owner, String text) {
        Note note = new Note(lastId.incrementAndGet(), owner, text.strip());
        notes.put(note.id(), note);
        return note;
    }

    public List<Note> ownedBy(String owner) {
        return notes.values().stream().filter(note -> note.owner().equals(owner)).toList();
    }

    /** The note if {@code user} owns it or is an admin; NoteNotFoundException otherwise. */
    public Note get(long id, String user, boolean admin) {
        Note note = notes.get(id);
        if (note == null || !(admin || note.owner().equals(user))) {
            throw new NoteNotFoundException(id);
        }
        return note;
    }

    public void delete(long id, String user, boolean admin) {
        notes.remove(get(id, user, admin).id());
    }

    public List<Note> all() {
        return List.copyOf(notes.values());
    }

    public int count() {
        return notes.size();
    }
}
