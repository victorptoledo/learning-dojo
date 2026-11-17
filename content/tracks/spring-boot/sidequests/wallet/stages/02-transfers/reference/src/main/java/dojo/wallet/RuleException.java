package dojo.wallet;

/** A valid request that the rules of the wallet refuse: 422. */
public class RuleException extends RuntimeException {
    public RuleException(String message) {
        super(message);
    }
}
