package dojo;

/** Thrown by every call to a node that is down, except position(), lastEpoch() and reads(). */
public class NodeDownException extends RuntimeException {
    public NodeDownException(String node) {
        super(node + " is down");
    }
}
