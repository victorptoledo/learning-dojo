package dojo;

import java.math.BigDecimal;

/** Published when a rate is set. {@code previous} is null for a new currency. */
public record RateChanged(String code, BigDecimal previous, BigDecimal current, String by) {
    /** The line the audit trail keeps, for example {@code "USD 1.08 -> 1.1 by trader"}. */
    public String describe() {
        String now = current.stripTrailingZeros().toPlainString();
        if (previous == null) {
            return code + " set to " + now + " by " + by;
        }
        return code + " " + previous.stripTrailingZeros().toPlainString() + " -> " + now + " by " + by;
    }
}
