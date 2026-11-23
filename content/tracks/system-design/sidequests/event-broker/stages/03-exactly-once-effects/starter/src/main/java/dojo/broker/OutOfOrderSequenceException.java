package dojo.broker;

/** An idempotent producer skipped a sequence number: a message in between was lost. */
public class OutOfOrderSequenceException extends RuntimeException {

    public OutOfOrderSequenceException(String message) {
        super(message);
    }
}
