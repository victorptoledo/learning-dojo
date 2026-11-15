package dojo;

/** The provider could not take the payment right now. Trying again may work. */
public class GatewayUnavailableException extends RuntimeException {
    public GatewayUnavailableException(String message) {
        super(message);
    }
}
