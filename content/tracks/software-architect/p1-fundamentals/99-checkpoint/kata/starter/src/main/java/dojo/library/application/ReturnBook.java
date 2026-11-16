package dojo.library.application;

import dojo.library.adapters.ConsoleNotifier;
import dojo.library.adapters.InMemoryLoans;
import dojo.library.domain.Loan;
import java.time.LocalDate;

/** The use case: a book comes back, maybe late. */
public class ReturnBook {
    public long giveBack(String isbn) {
        Loan loan = InMemoryLoans.INSTANCE.byIsbn(isbn)
                .orElseThrow(() -> new IllegalArgumentException("not on loan: " + isbn));
        InMemoryLoans.INSTANCE.remove(isbn);
        long fee = loan.lateFeeCents(LocalDate.now());
        if (fee > 0) {
            ConsoleNotifier.send(loan.memberId(), "Late fee: " + fee + " cents");
        }
        return fee;
    }
}
