package dojo.library.adapters;

import dojo.library.domain.Loan;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** The open loans, in memory, shared by everyone through one static instance. */
public class InMemoryLoans {
    public static final InMemoryLoans INSTANCE = new InMemoryLoans();

    private final List<Loan> loans = new ArrayList<>();

    public List<Loan> byMember(String memberId) {
        return loans.stream().filter(loan -> loan.memberId().equals(memberId)).toList();
    }

    public Optional<Loan> byIsbn(String isbn) {
        return loans.stream().filter(loan -> loan.isbn().equals(isbn)).findFirst();
    }

    public void add(Loan loan) {
        loans.add(loan);
    }

    public void remove(String isbn) {
        loans.removeIf(loan -> loan.isbn().equals(isbn));
    }
}
