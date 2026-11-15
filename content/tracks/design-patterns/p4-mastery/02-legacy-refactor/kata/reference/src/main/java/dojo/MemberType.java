package dojo;

import java.util.Arrays;
import java.util.Locale;
import java.util.function.LongUnaryOperator;

/** Replaces the "student" / "staff" / "guest" strings: every rule that varies by type lives here. */
public enum MemberType {
    STUDENT(3, 14, daysLate -> Math.min(500, daysLate * 10)),
    STAFF(10, 28, daysLate -> 0),
    GUEST(1, 7, daysLate -> daysLate * 50);

    private final int maxLoans;
    private final int loanDays;
    private final LongUnaryOperator lateFee;

    MemberType(int maxLoans, int loanDays, LongUnaryOperator lateFee) {
        this.maxLoans = maxLoans;
        this.loanDays = loanDays;
        this.lateFee = lateFee;
    }

    public static MemberType parse(String text) {
        return Arrays.stream(values())
                .filter(type -> type.name().toLowerCase(Locale.ROOT).equals(text))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("unknown member type: " + text));
    }

    public int maxLoans() {
        return maxLoans;
    }

    public int loanDays() {
        return loanDays;
    }

    public long lateFeeCents(long daysLate) {
        return lateFee.applyAsLong(daysLate);
    }
}
