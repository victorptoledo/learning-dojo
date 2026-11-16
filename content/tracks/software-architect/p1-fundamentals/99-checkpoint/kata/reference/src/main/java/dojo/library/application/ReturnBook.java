package dojo.library.application;

import dojo.library.domain.Loan;
import dojo.library.domain.Loans;
import dojo.library.domain.Notifier;
import java.time.Clock;
import java.time.LocalDate;

/** The use case: a book comes back, maybe late. */
public class ReturnBook {
    private final Loans loans;
    private final Notifier notifier;
    private final Clock clock;

    public ReturnBook(Loans loans, Notifier notifier, Clock clock) {
        this.loans = loans;
        this.notifier = notifier;
        this.clock = clock;
    }

    public long giveBack(String isbn) {
        Loan loan = loans.byIsbn(isbn)
                .orElseThrow(() -> new IllegalArgumentException("not on loan: " + isbn));
        loans.remove(isbn);
        long fee = loan.lateFeeCents(LocalDate.now(clock));
        if (fee > 0) {
            notifier.send(loan.memberId(), "Late fee: " + fee + " cents");
        }
        return fee;
    }
}
