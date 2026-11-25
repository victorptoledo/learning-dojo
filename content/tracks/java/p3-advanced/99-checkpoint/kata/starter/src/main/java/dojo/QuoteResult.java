package dojo;

/** What one carrier answered for one shipment. Already written. */
public sealed interface QuoteResult {
    record Quoted(Quote quote) implements QuoteResult {}

    record Failed(String carrier, String reason) implements QuoteResult {}

    record TimedOut(String carrier) implements QuoteResult {}
}
