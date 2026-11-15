package dojo;

/** The provider refused the payment. Trying again will not help. */
public class PaymentDeclinedException extends RuntimeException {
    public PaymentDeclinedException(String message) {
        super(message);
    }
}
