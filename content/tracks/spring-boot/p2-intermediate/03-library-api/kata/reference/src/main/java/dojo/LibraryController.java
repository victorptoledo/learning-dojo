package dojo;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
public class LibraryController {
    private final LibraryService library;

    public LibraryController(LibraryService library) {
        this.library = library;
    }

    @PostMapping("/books")
    public ResponseEntity<BookView> add(@Valid @RequestBody BookRequest request, UriComponentsBuilder uri) {
        BookView book = library.add(request);
        return ResponseEntity.created(uri.path("/books/{isbn}").build(book.isbn())).body(book);
    }

    @GetMapping("/books/{isbn}")
    public BookView get(@PathVariable String isbn) {
        return library.get(isbn);
    }

    @GetMapping("/books")
    public List<BookView> search(@RequestParam String q) {
        return library.search(q);
    }

    @GetMapping("/authors/{name}/books")
    public PageView<BookView> byAuthor(@PathVariable String name,
                                       @RequestParam(defaultValue = "0") int page,
                                       @RequestParam(defaultValue = "10") int size) {
        return library.byAuthor(name, page, size);
    }

    @GetMapping("/authors/stats")
    public List<AuthorStats> stats() {
        return library.stats();
    }
}
