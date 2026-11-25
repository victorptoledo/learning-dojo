package dojo;

/** A cell could not serve (it crashed, timed out or refused the connection). */
public class CellDownException extends RuntimeException {
    public CellDownException(String cell) {
        super(cell + " cannot serve");
    }
}
