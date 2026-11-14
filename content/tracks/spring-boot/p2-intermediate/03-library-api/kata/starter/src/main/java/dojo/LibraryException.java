package dojo;

import org.springframework.http.HttpStatus;

public class LibraryException extends RuntimeException {
    private final HttpStatus status;

    private LibraryException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus status() {
        return status;
    }

    public static LibraryException bookNotFound(String isbn) {
        return new LibraryException(HttpStatus.NOT_FOUND, "book " + isbn + " not found");
    }

    public static LibraryException duplicateIsbn(String isbn) {
        return new LibraryException(HttpStatus.CONFLICT, "book " + isbn + " already exists");
    }
}
