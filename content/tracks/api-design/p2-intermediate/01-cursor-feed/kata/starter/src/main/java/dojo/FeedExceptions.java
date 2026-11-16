package dojo;

/** The errors of the feed. Written for you; ApiProblems turns them into problem details. */
public final class FeedExceptions {
    private FeedExceptions() {}

    /** A cursor that cannot be decoded, was tampered with, or belongs to another query. */
    public static final class InvalidCursorException extends RuntimeException {
        public InvalidCursorException(String message) {
            super(message);
        }
    }

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
