package dojo;

import java.util.List;
import java.util.Optional;

/** How to pick the best quote: a strategy. Each factory below returns a lambda. */
@FunctionalInterface
public interface Choice {
    Optional<Quote> pick(List<Quote> quotes);

    /** Lowest price; ties go to fewer days, then to the carrier name in alphabetical order. */
    static Choice cheapest() {
        throw new UnsupportedOperationException("write Choice.cheapest");
    }

    /** Fewest days; ties go to the lower price, then to the carrier name. */
    static Choice fastest() {
        throw new UnsupportedOperationException("write Choice.fastest");
    }

    /** The cheapest of the quotes that arrive in at most {@code maxDays} days. */
    static Choice cheapestWithin(int maxDays) {
        throw new UnsupportedOperationException("write Choice.cheapestWithin");
    }
}
