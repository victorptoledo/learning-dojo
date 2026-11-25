package dojo;

import java.util.List;
import java.util.Optional;

/** Every carrier's result for one shipment, in carrier order, and the chosen quote. Already written. */
public record QuoteBoard(Shipment shipment, List<QuoteResult> results, Optional<Quote> best) {
    public QuoteBoard {
        results = List.copyOf(results);
    }
}
