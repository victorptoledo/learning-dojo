package dojo.cluster;

/** A cache node did not answer. Used from stage 4. */
public class NodeDownException extends RuntimeException {

    public NodeDownException(String message) {
        super(message);
    }
}
