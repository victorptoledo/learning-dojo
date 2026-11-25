package dojo;

/** The problems of the billing API; {@link ApiProblems} turns each into a problem detail. */
final class ApiExceptions {
    private ApiExceptions() {}

    static final class InvoiceNotFoundException extends RuntimeException {
        InvoiceNotFoundException(long id) {
            super("no invoice " + id);
        }
    }

    static final class InvalidParameterException extends RuntimeException {
        InvalidParameterException(String detail) {
            super(detail);
        }
    }

    static final class InvalidCursorException extends RuntimeException {
        InvalidCursorException() {
            super("this cursor was not issued by this API; start again without one");
        }
    }

    static final class KeyMissingException extends RuntimeException {
        KeyMissingException() {
            super("payments need an Idempotency-Key header, so that a retry cannot charge twice");
        }
    }

    static final class KeyReusedException extends RuntimeException {
        KeyReusedException() {
            super("this Idempotency-Key was already used for a different payment");
        }
    }

    static final class AlreadyPaidException extends RuntimeException {
        AlreadyPaidException(long id) {
            super("invoice " + id + " is already paid");
        }
    }

    static final class AmountMismatchException extends RuntimeException {
        AmountMismatchException(long expected) {
            super("the payment must be for the invoice's amount, " + expected + " cents");
        }
    }
}
