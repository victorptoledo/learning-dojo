package dojo;

/** Returned by {@link QuoteEngine#subscribe}; closing it stops the notifications. */
@FunctionalInterface
public interface Subscription extends AutoCloseable {
    @Override
    void close();
}
