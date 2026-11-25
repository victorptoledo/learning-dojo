package dojo;

/** A remote service refused the step for a business reason (out of stock, card declined). */
public class StepFailedException extends RuntimeException {
    public StepFailedException(String reason) {
        super(reason);
    }
}
