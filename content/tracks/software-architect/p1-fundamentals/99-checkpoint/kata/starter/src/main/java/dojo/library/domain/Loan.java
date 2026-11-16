package dojo.library.domain;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/** A book lent to a member. Do not change it. */
public record Loan(String isbn, String memberId, LocalDate borrowedOn, LocalDate dueOn) {
    public boolean isOverdue(LocalDate today) {
        return today.isAfter(dueOn);
    }

    /** 25 cents per day after the due date, at most 1000. */
    public long lateFeeCents(LocalDate returnedOn) {
        long daysLate = Math.max(0, ChronoUnit.DAYS.between(dueOn, returnedOn));
        return Math.min(1000, daysLate * 25);
    }
}
