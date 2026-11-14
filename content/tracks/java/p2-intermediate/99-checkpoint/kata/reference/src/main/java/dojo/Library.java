package dojo;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/** Books, members and loans. Loans last {@link #LOAN_DAYS} days. */
public class Library {
    public static final int LOAN_DAYS = 14;

    private final Supplier<LocalDate> today;
    private final InMemoryRepository<String, Book> books = new InMemoryRepository<>();
    private final InMemoryRepository<String, Member> members = new InMemoryRepository<>();
    private final InMemoryRepository<String, Loan> loans = new InMemoryRepository<>();

    public Library(Supplier<LocalDate> today) {
        this.today = today;
    }

    public void addBook(Book book) {
        books.save(book);
    }

    public void addMember(Member member) {
        members.save(member);
    }

    public Loan lend(String isbn, String memberId) {
        Book book = books.findById(isbn).orElseThrow(() -> new LibraryException.NotFound("book", isbn));
        Member member = members.findById(memberId)
                .orElseThrow(() -> new LibraryException.NotFound("member", memberId));
        if (loans.findById(book.isbn()).isPresent()) {
            throw new LibraryException.NotAvailable(isbn);
        }
        int held = loans.findAll(loan -> loan.memberId().equals(memberId)).size();
        if (held >= member.maxLoans()) {
            throw new LibraryException.LoanLimitReached(memberId, held);
        }
        return loans.save(new Loan(isbn, memberId, today.get().plusDays(LOAN_DAYS)));
    }

    public void giveBack(String isbn) {
        if (!loans.delete(isbn)) {
            throw new LibraryException.NotFound("loan", isbn);
        }
    }

    public List<Book> available() {
        return books.findAll(book -> loans.findById(book.isbn()).isEmpty()).stream()
                .sorted(Comparator.comparing(Book::title))
                .toList();
    }

    public List<Loan> overdue() {
        LocalDate now = today.get();
        return loans.findAll(loan -> loan.due().isBefore(now)).stream()
                .sorted(Comparator.comparing(Loan::due).thenComparing(Loan::isbn))
                .toList();
    }

    public Map<String, List<String>> titlesByMember() {
        return loans.findAll().stream()
                .collect(Collectors.groupingBy(Loan::memberId, TreeMap::new,
                        Collectors.collectingAndThen(
                                Collectors.mapping(loan -> title(loan.isbn()), Collectors.toList()),
                                titles -> titles.stream().sorted().toList())));
    }

    private String title(String isbn) {
        return books.findById(isbn).map(Book::title).orElse(isbn);
    }
}
