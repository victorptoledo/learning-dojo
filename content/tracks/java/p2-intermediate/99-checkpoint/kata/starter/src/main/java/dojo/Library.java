package dojo;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

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
        throw new UnsupportedOperationException("todo lend " + isbn + " to " + memberId);
    }

    public void giveBack(String isbn) {
        throw new UnsupportedOperationException("todo give back " + isbn);
    }

    public List<Book> available() {
        throw new UnsupportedOperationException("todo available books");
    }

    public List<Loan> overdue() {
        throw new UnsupportedOperationException("todo overdue loans on " + today.get());
    }

    public Map<String, List<String>> titlesByMember() {
        throw new UnsupportedOperationException("todo titles by member");
    }
}
