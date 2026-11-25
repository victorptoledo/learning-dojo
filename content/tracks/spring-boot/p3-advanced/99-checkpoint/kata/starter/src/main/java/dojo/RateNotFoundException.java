package dojo;

public class RateNotFoundException extends RuntimeException {
    public RateNotFoundException(String code) {
        super("no rate for " + code);
    }
}
