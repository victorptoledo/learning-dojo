package dojo.library.domain;

import java.util.List;
import java.util.Optional;

/** Driven port: the open loans. */
public interface Loans {
    List<Loan> byMember(String memberId);

    Optional<Loan> byIsbn(String isbn);

    void add(Loan loan);

    void remove(String isbn);
}
