package dojo;

/** A channel could not deliver a message. */
public class DeliveryException extends RuntimeException {
    public DeliveryException(String message) {
        super(message);
    }
}
