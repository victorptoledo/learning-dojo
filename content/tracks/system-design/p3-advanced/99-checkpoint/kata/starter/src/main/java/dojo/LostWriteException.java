package dojo;

/** The session saw a write that a failover lost, so its guarantees cannot be kept any more. */
public class LostWriteException extends RuntimeException {
    public LostWriteException(int partition) {
        super("this session saw a write of partition " + partition + " that a failover lost: start a new session");
    }
}
