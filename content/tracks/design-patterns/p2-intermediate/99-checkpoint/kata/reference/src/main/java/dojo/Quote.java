package dojo;

import java.util.ArrayList;
import java.util.List;

/** A priced quote: who it is for, the final amount in cents and the adjustments applied, in order. */
public record Quote(String customer, long cents, List<String> adjustments) {
    public Quote {
        adjustments = List.copyOf(adjustments);
    }

    /** A copy with a new amount and one more adjustment. */
    public Quote adjusted(long newCents, String adjustment) {
        List<String> more = new ArrayList<>(adjustments);
        more.add(adjustment);
        return new Quote(customer, newCents, more);
    }
}
