package dojo.domain;

/** No exchange rate is known for a pair of currencies. */
public class RateUnavailableException extends RuntimeException {
    public RateUnavailableException(String message) {
        super(message);
    }
}
