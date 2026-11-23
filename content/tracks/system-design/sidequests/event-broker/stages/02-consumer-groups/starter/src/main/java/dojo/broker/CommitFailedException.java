package dojo.broker;

/** A commit from an old generation, or for a partition the member does not own: nothing was recorded. */
public class CommitFailedException extends RuntimeException {

    public CommitFailedException(String message) {
        super(message);
    }
}
