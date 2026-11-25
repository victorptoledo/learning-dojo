package dojo;

/** An order line that cannot be parsed. Already written. */
public class BadOrderException extends IllegalArgumentException {
    public BadOrderException(String message) {
        super(message);
    }

    public BadOrderException(String message, Throwable cause) {
        super(message, cause);
    }
}
