package dojo;

import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
public class NoteController {
    private final NoteService notes;

    public NoteController(NoteService notes) {
        this.notes = notes;
    }

    @GetMapping("/notes/count")
    public Map<String, Integer> count() {
        return Map.of("count", notes.count());
    }

    @PostMapping("/notes")
    public ResponseEntity<Note> create(@Valid @RequestBody NewNote body, Principal user, UriComponentsBuilder uri) {
        Note note = notes.create(user.getName(), body.text());
        return ResponseEntity.created(uri.path("/notes/{id}").build(note.id())).body(note);
    }

    @GetMapping("/notes")
    public List<Note> mine(Principal user) {
        return notes.ownedBy(user.getName());
    }

    @GetMapping("/notes/{id}")
    public Note one(@PathVariable long id, Authentication user) {
        return notes.get(id, user.getName(), isAdmin(user));
    }

    @DeleteMapping("/notes/{id}")
    public ResponseEntity<Void> delete(@PathVariable long id, Authentication user) {
        notes.delete(id, user.getName(), isAdmin(user));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/admin/notes")
    public List<Note> all() {
        return notes.all();
    }

    private static boolean isAdmin(Authentication user) {
        return user.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }
}
