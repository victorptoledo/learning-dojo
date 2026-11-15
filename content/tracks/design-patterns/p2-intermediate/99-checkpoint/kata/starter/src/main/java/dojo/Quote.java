package dojo;

import java.util.List;

/** A priced quote: who it is for, the final amount in cents and the adjustments applied, in order. */
public record Quote(String customer, long cents, List<String> adjustments) {
    public Quote {
        adjustments = List.copyOf(adjustments);
    }
}
