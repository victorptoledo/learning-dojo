package dojo;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * The library's whole back office in one class, grown over eight years. It works, and the
 * characterisation tests prove it. Your job is to make it changeable without changing what it
 * does. See README.md for the rules.
 */
public class LibraryService {
    // isbn -> {title, "in" or "out"}
    private final Map<String, String[]> books = new HashMap<>();
    // member id -> "student", "staff" or "guest"
    private final Map<String, String> members = new HashMap<>();
    // isbn -> {member id, due date}
    private final Map<String, Object[]> loans = new HashMap<>();
    // member id -> unpaid fines in cents
    private final Map<String, Long> fines = new HashMap<>();
    private final List<String> outbox = new ArrayList<>();

    public void addBook(String isbn, String title) {
        if (isbn == null || isbn.isBlank()) throw new IllegalArgumentException("isbn is required");
        if (title == null || title.isBlank()) throw new IllegalArgumentException("title is required");
        if (books.containsKey(isbn)) throw new IllegalArgumentException("book exists: " + isbn);
        books.put(isbn, new String[] {title, "in"});
    }

    public void addMember(String id, String type) {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("member id is required");
        if (!type.equals("student") && !type.equals("staff") && !type.equals("guest")) {
            throw new IllegalArgumentException("unknown member type: " + type);
        }
        if (members.containsKey(id)) throw new IllegalArgumentException("member exists: " + id);
        members.put(id, type);
        fines.put(id, 0L);
    }

    public LocalDate borrow(String memberId, String isbn, LocalDate today) {
        String type = members.get(memberId);
        if (type == null) throw new IllegalArgumentException("unknown member: " + memberId);
        String[] book = books.get(isbn);
        if (book == null) throw new IllegalArgumentException("unknown book: " + isbn);
        if (book[1].equals("out")) throw new IllegalStateException("already on loan: " + isbn);
        if (fines.get(memberId) > 500) throw new IllegalStateException("unpaid fines: " + memberId);
        int count = 0;
        for (Object[] loan : loans.values()) {
            if (loan[0].equals(memberId)) count++;
        }
        int max;
        if (type.equals("student")) {
            max = 3;
        } else if (type.equals("staff")) {
            max = 10;
        } else {
            max = 1;
        }
        if (count >= max) throw new IllegalStateException("loan limit reached: " + memberId);
        LocalDate due;
        if (type.equals("student")) {
            due = today.plusDays(14);
        } else if (type.equals("staff")) {
            due = today.plusDays(28);
        } else {
            due = today.plusDays(7);
        }
        book[1] = "out";
        loans.put(isbn, new Object[] {memberId, due});
        outbox.add(memberId + ": you borrowed \"" + book[0] + "\", due " + due);
        return due;
    }

    public long giveBack(String memberId, String isbn, LocalDate today) {
        Object[] loan = loans.get(isbn);
        if (loan == null || !loan[0].equals(memberId)) {
            throw new IllegalStateException(memberId + " has not borrowed " + isbn);
        }
        LocalDate due = (LocalDate) loan[1];
        long daysLate = Math.max(0, ChronoUnit.DAYS.between(due, today));
        String type = members.get(memberId);
        long fee;
        if (type.equals("student")) {
            fee = Math.min(500, daysLate * 10);
        } else if (type.equals("staff")) {
            fee = 0;
        } else {
            fee = daysLate * 50;
        }
        loans.remove(isbn);
        books.get(isbn)[1] = "in";
        fines.put(memberId, fines.get(memberId) + fee);
        if (fee > 0) {
            outbox.add(memberId + ": \"" + books.get(isbn)[0] + "\" was " + daysLate + " days late, fee "
                    + fee / 100 + "." + String.format("%02d", fee % 100));
        }
        return fee;
    }

    public void payFine(String memberId, long cents) {
        if (!members.containsKey(memberId)) throw new IllegalArgumentException("unknown member: " + memberId);
        if (cents <= 0) throw new IllegalArgumentException("payment must be positive");
        long owed = fines.get(memberId);
        if (cents > owed) throw new IllegalArgumentException("payment exceeds fines: " + owed);
        fines.put(memberId, owed - cents);
    }

    public long fines(String memberId) {
        if (!members.containsKey(memberId)) throw new IllegalArgumentException("unknown member: " + memberId);
        return fines.get(memberId);
    }

    public String overdueReport(LocalDate today) {
        Map<String, String> lines = new TreeMap<>();
        for (Map.Entry<String, Object[]> entry : loans.entrySet()) {
            LocalDate due = (LocalDate) entry.getValue()[1];
            if (due.isBefore(today)) {
                long days = ChronoUnit.DAYS.between(due, today);
                String memberId = (String) entry.getValue()[0];
                String title = books.get(entry.getKey())[0];
                lines.put(memberId + "|" + entry.getKey(), memberId + " " + title + " (" + days + " days)");
            }
        }
        if (lines.isEmpty()) return "Nothing overdue";
        StringBuilder out = new StringBuilder("Overdue on " + today + ":");
        for (String line : lines.values()) {
            out.append("\n- ").append(line);
        }
        return out.toString();
    }

    public List<String> outbox() {
        return List.copyOf(outbox);
    }
}
