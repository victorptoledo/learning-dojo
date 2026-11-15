package dojo;

/** The observer: told about every quote the engine produces. */
@FunctionalInterface
public interface QuoteListener {
    void quoted(QuoteRequest request, Quote quote);
}
