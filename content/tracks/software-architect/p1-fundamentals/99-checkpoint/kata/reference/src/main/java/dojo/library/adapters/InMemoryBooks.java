package dojo.library.adapters;

import dojo.library.domain.Book;
import dojo.library.domain.Books;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/** Driven adapter: the catalogue, in memory. */
public class InMemoryBooks implements Books {
    private final Map<String, Book> books = new LinkedHashMap<>();

    public void add(Book book) {
        books.put(book.isbn(), book);
    }

    @Override
    public Optional<Book> find(String isbn) {
        return Optional.ofNullable(books.get(isbn));
    }
}
