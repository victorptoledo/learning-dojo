package dojo;

/**
 * Stands for the process dying in the middle of a saga (a deploy, an out-of-memory kill). It is not a
 * business failure: nothing should catch it. Whatever was saved before it is all a later run has.
 */
public class CrashException extends RuntimeException {
    public CrashException(String message) {
        super(message);
    }
}
