package dojo.rooms;

/** A valid request that breaks a rule of the building: 422. */
public class RuleException extends RuntimeException {
    public RuleException(String message) {
        super(message);
    }
}
