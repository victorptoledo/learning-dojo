package dojo;

/** The rates service does not know this pair: the request is wrong, and retrying will not help. */
public class UnknownCurrencyException extends RuntimeException {
    public UnknownCurrencyException(String from, String to) {
        super("no rate from " + from + " to " + to);
    }
}
