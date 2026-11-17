package dojo.tickets;

/** A valid request that the rules of the sale refuse: 422. */
public class RuleException extends RuntimeException {
    public RuleException(String message) {
        super(message);
    }
}
