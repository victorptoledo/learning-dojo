package dojo;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LibraryService {
    private final BookRepository books;
    private final AuthorRepository authors;

    public LibraryService(BookRepository books, AuthorRepository authors) {
        this.books = books;
        this.authors = authors;
    }

    @Transactional
    public BookView add(BookRequest request) {
        String isbn = request.isbn().strip();
        if (books.existsByIsbn(isbn)) {
            throw LibraryException.duplicateIsbn(isbn);
        }
        String name = request.author().strip();
        Author author = authors.findByName(name).orElseGet(() -> authors.save(new Author(name)));
        return BookView.of(books.save(new Book(isbn, request.title().strip(), request.year(), author)));
    }

    @Transactional(readOnly = true)
    public BookView get(String isbn) {
        return books.findByIsbn(isbn).map(BookView::of).orElseThrow(() -> LibraryException.bookNotFound(isbn));
    }

    @Transactional(readOnly = true)
    public List<BookView> search(String text) {
        return books.search(text.strip()).stream().map(BookView::of).toList();
    }

    @Transactional(readOnly = true)
    public PageView<BookView> byAuthor(String author, int page, int size) {
        Page<Book> found = books.findByAuthorName(author,
                PageRequest.of(page, size, Sort.by("year").and(Sort.by("title"))));
        return new PageView<>(found.map(BookView::of).getContent(), page, size, found.getTotalElements());
    }

    @Transactional(readOnly = true)
    public List<AuthorStats> stats() {
        return books.stats();
    }
}
