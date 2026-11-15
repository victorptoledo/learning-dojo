package dojo;

/** Thrown by a replica that cannot be reached. */
public class ReplicaDownException extends RuntimeException {
    public ReplicaDownException(String name) {
        super(name + " is down");
    }
}
