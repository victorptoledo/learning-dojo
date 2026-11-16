package dojo.library;

import dojo.library.adapters.InMemoryBooks;
import dojo.library.application.BorrowBook;
import dojo.library.application.ReturnBook;

/** The composition root. */
public final class App {
    private App() {}

    public static Library create() {
        InMemoryBooks books = new InMemoryBooks();
        return new Library(new BorrowBook(books), new ReturnBook(), books);
    }
}
