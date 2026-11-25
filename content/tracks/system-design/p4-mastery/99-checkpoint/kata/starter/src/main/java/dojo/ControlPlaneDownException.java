package dojo;

/** The control plane cannot answer right now. */
public class ControlPlaneDownException extends RuntimeException {
    public ControlPlaneDownException(String message) {
        super(message);
    }
}
