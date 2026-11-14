package dojo;

import org.springframework.http.HttpStatus;

public class PaymentException extends RuntimeException {
    private final HttpStatus status;

    public PaymentException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus status() {
        return status;
    }

    public static PaymentException keyReused() {
        return new PaymentException(HttpStatus.UNPROCESSABLE_CONTENT, "this Idempotency-Key was used for a different request");
    }

    public static PaymentException inProgress() {
        return new PaymentException(HttpStatus.CONFLICT, "a request with this Idempotency-Key is still in progress");
    }

    public static PaymentException declined(String message) {
        return new PaymentException(HttpStatus.PAYMENT_REQUIRED, message);
    }
}
