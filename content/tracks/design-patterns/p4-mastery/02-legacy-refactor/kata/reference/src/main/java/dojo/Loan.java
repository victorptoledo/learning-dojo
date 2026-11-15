package dojo;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public record Loan(Member member, Book book, LocalDate due) {
    public long daysLate(LocalDate today) {
        return Math.max(0, ChronoUnit.DAYS.between(due, today));
    }
}
