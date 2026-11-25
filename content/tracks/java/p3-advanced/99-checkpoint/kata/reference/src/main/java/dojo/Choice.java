package dojo;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** How to pick the best quote: a strategy. Each factory below returns a lambda. */
@FunctionalInterface
public interface Choice {
    Optional<Quote> pick(List<Quote> quotes);

    Comparator<Quote> BY_PRICE = Comparator.comparingLong(Quote::priceCents)
            .thenComparingInt(Quote::days)
            .thenComparing(Quote::carrier);

    Comparator<Quote> BY_DAYS = Comparator.comparingInt(Quote::days)
            .thenComparingLong(Quote::priceCents)
            .thenComparing(Quote::carrier);

    /** Lowest price; ties go to fewer days, then to the carrier name in alphabetical order. */
    static Choice cheapest() {
        return quotes -> quotes.stream().min(BY_PRICE);
    }

    /** Fewest days; ties go to the lower price, then to the carrier name. */
    static Choice fastest() {
        return quotes -> quotes.stream().min(BY_DAYS);
    }

    /** The cheapest of the quotes that arrive in at most {@code maxDays} days. */
    static Choice cheapestWithin(int maxDays) {
        return quotes -> quotes.stream().filter(quote -> quote.days() <= maxDays).min(BY_PRICE);
    }
}
