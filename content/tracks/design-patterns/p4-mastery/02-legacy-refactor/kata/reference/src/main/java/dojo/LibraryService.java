package dojo;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** The library's back office: it coordinates; the rules live in MemberType, Loan and OverdueReport. */
public class LibraryService {
    private static final long FINE_LIMIT_CENTS = 500;

    private final Map<String, Book> books = new HashMap<>();
    private final Map<String, Member> members = new HashMap<>();
    private final Map<String, Loan> loans = new HashMap<>();
    private final Map<String, Long> fines = new HashMap<>();
    private final List<String> outbox = new ArrayList<>();

    public void addBook(String isbn, String title) {
        requireText(isbn, "isbn is required");
        requireText(title, "title is required");
        if (books.containsKey(isbn)) throw new IllegalArgumentException("book exists: " + isbn);
        books.put(isbn, new Book(isbn, title));
    }

    public void addMember(String id, String type) {
        requireText(id, "member id is required");
        MemberType memberType = MemberType.parse(type);
        if (members.containsKey(id)) throw new IllegalArgumentException("member exists: " + id);
        members.put(id, new Member(id, memberType));
        fines.put(id, 0L);
    }

    public LocalDate borrow(String memberId, String isbn, LocalDate today) {
        Member member = member(memberId);
        Book book = book(isbn);
        if (loans.containsKey(isbn)) throw new IllegalStateException("already on loan: " + isbn);
        if (fines.get(memberId) > FINE_LIMIT_CENTS) throw new IllegalStateException("unpaid fines: " + memberId);
        if (loansOf(member) >= member.type().maxLoans()) throw new IllegalStateException("loan limit reached: " + memberId);
        Loan loan = new Loan(member, book, today.plusDays(member.type().loanDays()));
        loans.put(isbn, loan);
        outbox.add(memberId + ": you borrowed \"" + book.title() + "\", due " + loan.due());
        return loan.due();
    }

    public long giveBack(String memberId, String isbn, LocalDate today) {
        Loan loan = loans.get(isbn);
        if (loan == null || !loan.member().id().equals(memberId)) {
            throw new IllegalStateException(memberId + " has not borrowed " + isbn);
        }
        long daysLate = loan.daysLate(today);
        long fee = loan.member().type().lateFeeCents(daysLate);
        loans.remove(isbn);
        fines.merge(memberId, fee, Long::sum);
        if (fee > 0) {
            outbox.add(memberId + ": \"" + loan.book().title() + "\" was " + daysLate + " days late, fee " + Money.format(fee));
        }
        return fee;
    }

    public void payFine(String memberId, long cents) {
        member(memberId);
        if (cents <= 0) throw new IllegalArgumentException("payment must be positive");
        long owed = fines.get(memberId);
        if (cents > owed) throw new IllegalArgumentException("payment exceeds fines: " + owed);
        fines.put(memberId, owed - cents);
    }

    public long fines(String memberId) {
        member(memberId);
        return fines.get(memberId);
    }

    public String overdueReport(LocalDate today) {
        return OverdueReport.render(loans.values(), today);
    }

    public List<String> outbox() {
        return List.copyOf(outbox);
    }

    private Member member(String id) {
        Member member = members.get(id);
        if (member == null) throw new IllegalArgumentException("unknown member: " + id);
        return member;
    }

    private Book book(String isbn) {
        Book book = books.get(isbn);
        if (book == null) throw new IllegalArgumentException("unknown book: " + isbn);
        return book;
    }

    private long loansOf(Member member) {
        return loans.values().stream().filter(loan -> loan.member().equals(member)).count();
    }

    private static void requireText(String text, String message) {
        if (text == null || text.isBlank()) throw new IllegalArgumentException(message);
    }
}
