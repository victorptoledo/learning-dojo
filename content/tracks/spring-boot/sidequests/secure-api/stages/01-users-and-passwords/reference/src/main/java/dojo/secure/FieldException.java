package dojo.secure;

/** One bad field found after Bean Validation: 400 with the errors object. */
public class FieldException extends RuntimeException {
    private final String field;

    public FieldException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String field() {
        return field;
    }
}
