package dojo;

/** The circuit is open: the call was not even attempted. */
public class CircuitOpenException extends RuntimeException {
    public CircuitOpenException() {
        super("circuit open");
    }
}
