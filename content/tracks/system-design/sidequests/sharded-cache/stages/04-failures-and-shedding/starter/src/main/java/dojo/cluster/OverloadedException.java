package dojo.cluster;

/** Too many database loads are running: the read is refused at once instead of queueing. */
public class OverloadedException extends RuntimeException {

    public OverloadedException(String message) {
        super(message);
    }
}
