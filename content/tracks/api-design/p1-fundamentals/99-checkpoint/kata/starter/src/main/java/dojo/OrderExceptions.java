package dojo;

/** The errors of the orders API. Written for you. */
public final class OrderExceptions {
    private OrderExceptions() {}

    public static final class OrderNotFoundException extends RuntimeException {
        public OrderNotFoundException(long id) {
            super("order " + id + " does not exist");
        }
    }

    public static final class OrderNotCancellableException extends RuntimeException {
        public OrderNotCancellableException(long id, OrderStatus status) {
            super("order " + id + " is " + status + " and can no longer be cancelled");
        }
    }

    /** A query parameter with a value the API does not accept. */
    public static final class InvalidParameterException extends RuntimeException {
        private final String parameter;

        public InvalidParameterException(String parameter, String message) {
            super(message);
            this.parameter = parameter;
        }

        public String parameter() {
            return parameter;
        }
    }
}
