package dojo.broker;

/** A read asked for an offset the partition does not have (deleted by retention, or not written yet). */
public class OffsetOutOfRangeException extends RuntimeException {

    public OffsetOutOfRangeException(String message) {
        super(message);
    }
}
