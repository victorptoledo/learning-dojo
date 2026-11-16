package dojo.library.application;

import dojo.library.domain.Books;
import dojo.library.domain.Loan;
import dojo.library.domain.LoanPolicy;
import dojo.library.domain.LoanRefusedException;
import dojo.library.domain.Loans;
import dojo.library.domain.Notifier;
import java.time.Clock;
import java.time.LocalDate;

/** The use case: a member borrows a book. */
public class BorrowBook {
    private final Books books;
    private final Loans loans;
    private final Notifier notifier;
    private final Clock clock;
    private final LoanPolicy policy = new LoanPolicy();

    public BorrowBook(Books books, Loans loans, Notifier notifier, Clock clock) {
        this.books = books;
        this.loans = loans;
        this.notifier = notifier;
        this.clock = clock;
    }

    public Loan borrow(String memberId, String isbn) {
        books.find(isbn).orElseThrow(() -> new IllegalArgumentException("unknown book " + isbn));
        if (loans.byIsbn(isbn).isPresent()) {
            throw new LoanRefusedException("already on loan: " + isbn);
        }
        LocalDate today = LocalDate.now(clock);
        policy.checkCanBorrow(loans.byMember(memberId), today);
        Loan loan = new Loan(isbn, memberId, today, policy.dueDate(today));
        loans.add(loan);
        notifier.send(memberId, "Due back on " + loan.dueOn());
        return loan;
    }
}
