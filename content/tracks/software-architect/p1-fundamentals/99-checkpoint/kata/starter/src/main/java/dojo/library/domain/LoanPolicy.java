package dojo.library.domain;

import dojo.library.adapters.InMemoryLoans;
import java.time.LocalDate;
import java.util.List;

/** Who may borrow, and for how long. */
public class LoanPolicy {
    public static final int MAX_LOANS = 3;
    public static final int LOAN_DAYS = 14;

    public void checkCanBorrow(String memberId) {
        List<Loan> current = InMemoryLoans.INSTANCE.byMember(memberId);
        LocalDate today = LocalDate.now();
        if (current.stream().anyMatch(loan -> loan.isOverdue(today))) {
            throw new LoanRefusedException("member has an overdue loan");
        }
        if (current.size() >= MAX_LOANS) {
            throw new LoanRefusedException("member has " + MAX_LOANS + " loans");
        }
    }

    public LocalDate dueDate() {
        return LocalDate.now().plusDays(LOAN_DAYS);
    }
}
