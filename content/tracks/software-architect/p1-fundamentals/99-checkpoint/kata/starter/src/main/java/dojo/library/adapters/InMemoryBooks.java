package dojo.library.adapters;

import dojo.library.domain.Book;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/** The catalogue, in memory. */
public class InMemoryBooks {
    private final Map<String, Book> books = new LinkedHashMap<>();

    public void add(Book book) {
        books.put(book.isbn(), book);
    }

    public Optional<Book> find(String isbn) {
        return Optional.ofNullable(books.get(isbn));
    }
}
