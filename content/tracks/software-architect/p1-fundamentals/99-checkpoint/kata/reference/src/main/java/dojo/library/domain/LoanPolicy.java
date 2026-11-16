package dojo.library.domain;

import java.time.LocalDate;
import java.util.List;

/** Who may borrow, and for how long. Pure: everything it needs comes in as arguments. */
public class LoanPolicy {
    public static final int MAX_LOANS = 3;
    public static final int LOAN_DAYS = 14;

    public void checkCanBorrow(List<Loan> current, LocalDate today) {
        if (current.stream().anyMatch(loan -> loan.isOverdue(today))) {
            throw new LoanRefusedException("member has an overdue loan");
        }
        if (current.size() >= MAX_LOANS) {
            throw new LoanRefusedException("member has " + MAX_LOANS + " loans");
        }
    }

    public LocalDate dueDate(LocalDate borrowedOn) {
        return borrowedOn.plusDays(LOAN_DAYS);
    }
}
