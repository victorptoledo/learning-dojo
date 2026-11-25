package dojo;

import java.time.Duration;
import org.springframework.http.HttpStatus;

public class StatementsException extends RuntimeException {
    private final HttpStatus status;
    private final Duration retryAfter;

    private StatementsException(HttpStatus status, String message, Duration retryAfter) {
        super(message);
        this.status = status;
        this.retryAfter = retryAfter;
    }

    public HttpStatus status() {
        return status;
    }

    /** Null unless the client should come back later. */
    public Duration retryAfter() {
        return retryAfter;
    }

    /** The same answer whether the account does not exist or belongs to someone else. */
    public static StatementsException accountNotFound() {
        return new StatementsException(HttpStatus.NOT_FOUND, "account not found", null);
    }

    public static StatementsException badRequest(String message) {
        return new StatementsException(HttpStatus.BAD_REQUEST, message, null);
    }

    /** 503 with a Retry-After header in seconds. */
    public static StatementsException ratesUnavailable(Duration retryAfter) {
        return new StatementsException(HttpStatus.SERVICE_UNAVAILABLE, "exchange rates are unavailable", retryAfter);
    }
}
