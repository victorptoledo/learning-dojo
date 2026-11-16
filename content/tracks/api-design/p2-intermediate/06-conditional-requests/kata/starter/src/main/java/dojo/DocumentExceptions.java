package dojo;

/** The errors of the documents API. Written for you; ApiProblems maps them. */
public final class DocumentExceptions {
    private DocumentExceptions() {}

    /** 404. */
    public static final class DocumentNotFoundException extends RuntimeException {
        public DocumentNotFoundException(String id) {
            super("document " + id + " does not exist");
        }
    }

    /** 428: a change was sent without If-Match. */
    public static final class PreconditionRequiredException extends RuntimeException {
        public PreconditionRequiredException() {
            super("send If-Match with the ETag you last read, so a change cannot overwrite another one");
        }
    }

    /** 412: If-Match does not match the current ETag. */
    public static final class PreconditionFailedException extends RuntimeException {
        public PreconditionFailedException(String id) {
            super("document " + id + " changed since you read it; read it again and reapply your change");
        }
    }
}
