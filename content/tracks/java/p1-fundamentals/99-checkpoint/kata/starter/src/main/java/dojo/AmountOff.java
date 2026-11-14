package dojo;

/** {@code cents} off the whole order when its subtotal is at least {@code minimumCents}. */
public record AmountOff(long cents, long minimumCents) implements Discount {
    public AmountOff {
        if (cents <= 0 || minimumCents < 0) {
            throw new IllegalArgumentException("invalid amount off: " + cents + " from " + minimumCents);
        }
    }
}
