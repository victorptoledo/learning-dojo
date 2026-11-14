package dojo;

/** {@code percent}% off every item of {@code category}. */
public record PercentOff(String category, int percent) implements Discount {
    public PercentOff {
        if (percent < 1 || percent > 100) {
            throw new IllegalArgumentException("percent must be 1..100: " + percent);
        }
    }
}
