package dojo;

import java.math.BigDecimal;
import java.time.Instant;

/** What the API answers, and what the cache keeps (never the entity itself). */
public record RateView(String code, BigDecimal perEuro, Instant updatedAt) {
    public static RateView of(Rate rate) {
        return new RateView(rate.getCode(), rate.getPerEuro().stripTrailingZeros(), rate.getUpdatedAt());
    }
}
