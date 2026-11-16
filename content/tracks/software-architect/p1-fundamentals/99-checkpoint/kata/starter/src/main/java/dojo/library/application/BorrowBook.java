package dojo.library.application;

import dojo.library.adapters.ConsoleNotifier;
import dojo.library.adapters.InMemoryBooks;
import dojo.library.adapters.InMemoryLoans;
import dojo.library.domain.Loan;
import dojo.library.domain.LoanPolicy;
import dojo.library.domain.LoanRefusedException;
import java.time.LocalDate;

/** The use case: a member borrows a book. */
public class BorrowBook {
    private final InMemoryBooks books;
    private final LoanPolicy policy = new LoanPolicy();

    public BorrowBook(InMemoryBooks books) {
        this.books = books;
    }

    public Loan borrow(String memberId, String isbn) {
        books.find(isbn).orElseThrow(() -> new IllegalArgumentException("unknown book " + isbn));
        if (InMemoryLoans.INSTANCE.byIsbn(isbn).isPresent()) {
            throw new LoanRefusedException("already on loan: " + isbn);
        }
        policy.checkCanBorrow(memberId);
        Loan loan = new Loan(isbn, memberId, LocalDate.now(), policy.dueDate());
        InMemoryLoans.INSTANCE.add(loan);
        System.out.println("loan of " + isbn + " to " + memberId);
        ConsoleNotifier.send(memberId, "Due back on " + loan.dueOn());
        return loan;
    }
}
