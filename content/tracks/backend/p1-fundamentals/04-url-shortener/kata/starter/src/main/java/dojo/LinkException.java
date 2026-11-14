package dojo;

import org.springframework.http.HttpStatus;

public class LinkException extends RuntimeException {
    private final HttpStatus status;

    private LinkException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus status() {
        return status;
    }

    public static LinkException invalidUrl(String url) {
        return new LinkException(HttpStatus.BAD_REQUEST, "not an http(s) url: " + url);
    }

    public static LinkException invalidCode(String code) {
        return new LinkException(HttpStatus.BAD_REQUEST, "codes are 3 to 20 letters, digits or dashes: " + code);
    }

    public static LinkException codeTaken(String code) {
        return new LinkException(HttpStatus.CONFLICT, "code " + code + " is taken");
    }

    public static LinkException notFound(String code) {
        return new LinkException(HttpStatus.NOT_FOUND, "no link " + code);
    }
}
