package dojo;

/** What can go wrong with an idempotency key. Written for you; ApiProblems maps them. */
public final class IdempotencyExceptions {
    private IdempotencyExceptions() {}

    /** 400: the request has no Idempotency-Key header, or a blank one. */
    public static final class KeyMissingException extends RuntimeException {
        public KeyMissingException() {
            super("this operation needs an Idempotency-Key header");
        }
    }

    /** 422: the key was already used for a different request. */
    public static final class KeyReusedException extends RuntimeException {
        public KeyReusedException(String key) {
            super("idempotency key " + key + " was already used for a different request");
        }
    }

    /** 409: the first request with this key has not finished yet. */
    public static final class RequestInProgressException extends RuntimeException {
        public RequestInProgressException(String key) {
            super("a request with idempotency key " + key + " is still being processed");
        }
    }
}
