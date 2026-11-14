package dojo;

import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class LibraryService {
    private final BookRepository books;
    private final AuthorRepository authors;

    public LibraryService(BookRepository books, AuthorRepository authors) {
        this.books = books;
        this.authors = authors;
    }

    public BookView add(BookRequest request) {
        throw new UnsupportedOperationException("todo add " + request.isbn());
    }

    public BookView get(String isbn) {
        throw new UnsupportedOperationException("todo get " + isbn);
    }

    public List<BookView> search(String text) {
        throw new UnsupportedOperationException("todo search " + text);
    }

    public PageView<BookView> byAuthor(String author, int page, int size) {
        throw new UnsupportedOperationException("todo books of " + author);
    }

    public List<AuthorStats> stats() {
        throw new UnsupportedOperationException("todo stats");
    }
}
