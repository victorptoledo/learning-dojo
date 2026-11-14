package dojo;

import java.time.LocalDate;

/** A book lent to a member until {@code due}. */
public record Loan(String isbn, String memberId, LocalDate due) implements Identified<String> {
    @Override
    public String id() {
        return isbn;
    }
}
