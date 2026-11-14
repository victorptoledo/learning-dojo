package dojo;

/** Every way a library operation can be refused. */
public sealed class LibraryException extends RuntimeException
        permits LibraryException.NotFound, LibraryException.NotAvailable, LibraryException.LoanLimitReached {

    private LibraryException(String message) {
        super(message);
    }

    /** "book 123 not found", "member m1 not found", "loan 123 not found". */
    public static final class NotFound extends LibraryException {
        public NotFound(String what, String id) {
            super(what + " " + id + " not found");
        }
    }

    /** "book 123 is already lent". */
    public static final class NotAvailable extends LibraryException {
        public NotAvailable(String isbn) {
            super("book " + isbn + " is already lent");
        }
    }

    /** "member m1 already has 2 loans". */
    public static final class LoanLimitReached extends LibraryException {
        public LoanLimitReached(String memberId, int loans) {
            super("member " + memberId + " already has " + loans + " loans");
        }
    }
}
