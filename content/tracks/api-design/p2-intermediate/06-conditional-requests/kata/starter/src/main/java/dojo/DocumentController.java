package dojo;

import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The documents API. It works, but every GET sends the whole document again and the last
 * PUT silently wins over a change made in between.
 */
@RestController
@RequestMapping("/documents")
public class DocumentController {
    private final DocumentStore store;

    public DocumentController(DocumentStore store) {
        this.store = store;
    }

    @GetMapping("/{id}")
    public Document get(@PathVariable String id) {
        return store.get(id);
    }

    @PostMapping
    public ResponseEntity<Document> create(@Valid @RequestBody DocumentRequest request) {
        Document document = store.create(request);
        return ResponseEntity.created(URI.create("/documents/" + document.id())).body(document);
    }

    @PutMapping("/{id}")
    public Document replace(@PathVariable String id, @Valid @RequestBody DocumentRequest request) {
        return store.replace(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        store.delete(id);
        return ResponseEntity.noContent().build();
    }
}
