package dojo;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.StringJoiner;

/** The overdue report, on its own: it only reads loans. */
public final class OverdueReport {
    private OverdueReport() {}

    public static String render(Collection<Loan> loans, LocalDate today) {
        List<Loan> late = loans.stream()
                .filter(loan -> loan.daysLate(today) > 0)
                .sorted(Comparator.comparing((Loan loan) -> loan.member().id()).thenComparing(loan -> loan.book().isbn()))
                .toList();
        if (late.isEmpty()) {
            return "Nothing overdue";
        }
        var out = new StringJoiner("\n- ", "Overdue on " + today + ":\n- ", "");
        late.forEach(loan -> out.add(loan.member().id() + " " + loan.book().title() + " (" + loan.daysLate(today) + " days)"));
        return out.toString();
    }
}
