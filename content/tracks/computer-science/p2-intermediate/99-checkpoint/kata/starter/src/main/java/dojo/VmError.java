package dojo;

/** The program failed while running: division by zero, a broken stack, or too many steps. */
public class VmError extends RuntimeException {
    public VmError(String message) {
        super(message);
    }
}
