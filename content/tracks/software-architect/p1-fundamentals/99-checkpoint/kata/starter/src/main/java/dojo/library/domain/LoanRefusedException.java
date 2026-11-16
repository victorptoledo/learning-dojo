package dojo.library.domain;

public class LoanRefusedException extends RuntimeException {
    public LoanRefusedException(String message) {
        super(message);
    }
}
