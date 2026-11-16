package dojo;

/** A lexical, syntax or name error, with the 1-based line where it was found. */
public class CompileError extends RuntimeException {
    private final int line;

    public CompileError(String message, int line) {
        super("line " + line + ": " + message);
        this.line = line;
    }

    public int line() {
        return line;
    }
}
