package dojo;

/** Thrown when fewer replicas than the quorum answered. */
public class QuorumNotReachedException extends RuntimeException {
    public QuorumNotReachedException(String message) {
        super(message);
    }
}
