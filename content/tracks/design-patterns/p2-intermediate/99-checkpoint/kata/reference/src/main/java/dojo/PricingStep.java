package dojo;

/** One link of the pricing pipeline: returns the quote unchanged or adjusted. */
@FunctionalInterface
public interface PricingStep {
    Quote apply(QuoteRequest request, Quote quote);
}
