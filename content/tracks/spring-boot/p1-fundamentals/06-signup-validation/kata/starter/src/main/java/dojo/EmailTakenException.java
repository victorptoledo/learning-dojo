package dojo;

public class EmailTakenException extends RuntimeException {
    public EmailTakenException(String email) {
        super("email " + email + " is already registered");
    }
}
