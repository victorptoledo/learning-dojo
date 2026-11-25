package dojo;

/** The problems of the exports API; {@link ApiProblems} turns each into a problem detail. */
final class ApiExceptions {
    private ApiExceptions() {}

    static final class KeyInQueryException extends RuntimeException {}

    static final class UnauthorizedException extends RuntimeException {}

    static final class InsufficientScopeException extends RuntimeException {
        final String scope;

        InsufficientScopeException(String scope) {
            this.scope = scope;
        }
    }

    static final class RateLimitedException extends RuntimeException {
        final long retryAfterSeconds;

        RateLimitedException(long retryAfterSeconds) {
            this.retryAfterSeconds = retryAfterSeconds;
        }
    }

    static final class ExportNotFoundException extends RuntimeException {
        ExportNotFoundException(String id) {
            super("no export " + id);
        }
    }
}
