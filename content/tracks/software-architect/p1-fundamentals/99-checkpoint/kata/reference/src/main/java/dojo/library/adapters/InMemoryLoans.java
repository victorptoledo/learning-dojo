package dojo.library.adapters;

import dojo.library.domain.Loan;
import dojo.library.domain.Loans;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Driven adapter: the open loans, in memory. */
public class InMemoryLoans implements Loans {
    private final List<Loan> loans = new ArrayList<>();

    @Override
    public List<Loan> byMember(String memberId) {
        return loans.stream().filter(loan -> loan.memberId().equals(memberId)).toList();
    }

    @Override
    public Optional<Loan> byIsbn(String isbn) {
        return loans.stream().filter(loan -> loan.isbn().equals(isbn)).findFirst();
    }

    @Override
    public void add(Loan loan) {
        loans.add(loan);
    }

    @Override
    public void remove(String isbn) {
        loans.removeIf(loan -> loan.isbn().equals(isbn));
    }
}
