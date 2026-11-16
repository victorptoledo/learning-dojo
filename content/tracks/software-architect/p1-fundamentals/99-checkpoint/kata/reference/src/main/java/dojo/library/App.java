package dojo.library;

import dojo.library.adapters.InMemoryBooks;
import dojo.library.adapters.InMemoryLoans;
import dojo.library.application.BorrowBook;
import dojo.library.application.ReturnBook;
import dojo.library.domain.Notifier;
import java.time.Clock;

/** The composition root: the one place that knows the adapters. */
public final class App {
    private App() {}

    public static Library create(Clock clock, Notifier notifier) {
        InMemoryBooks books = new InMemoryBooks();
        InMemoryLoans loans = new InMemoryLoans();
        return new Library(
                new BorrowBook(books, loans, notifier, clock),
                new ReturnBook(loans, notifier, clock),
                books);
    }
}
