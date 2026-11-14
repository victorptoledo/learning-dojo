package dojo;

/** The first line of the input that cannot be read. */
public class BadLineException extends RuntimeException {
    private final int line;

    public BadLineException(int line, String reason, Throwable cause) {
        super("line " + line + ": " + reason, cause);
        this.line = line;
    }

    public BadLineException(int line, String reason) {
        this(line, reason, null);
    }

    public int line() {
        return line;
    }
}
