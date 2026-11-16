package dojo.library.domain;

import java.util.Optional;

/** Driven port: the catalogue. */
public interface Books {
    Optional<Book> find(String isbn);
}
