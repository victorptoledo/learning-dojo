package dojo;

import org.springframework.http.HttpStatus;

public class TrackerException extends RuntimeException {
    private final HttpStatus status;

    private TrackerException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus status() {
        return status;
    }

    public static TrackerException projectNotFound(String key) {
        return new TrackerException(HttpStatus.NOT_FOUND, "project " + key + " not found");
    }

    public static TrackerException issueNotFound(String key, int number) {
        return new TrackerException(HttpStatus.NOT_FOUND, "issue " + key + "-" + number + " not found");
    }

    public static TrackerException duplicateProject(String key) {
        return new TrackerException(HttpStatus.CONFLICT, "project " + key + " already exists");
    }

    public static TrackerException badRequest(String message) {
        return new TrackerException(HttpStatus.BAD_REQUEST, message);
    }

    public static TrackerException ifMatchRequired() {
        return new TrackerException(HttpStatus.PRECONDITION_REQUIRED, "send If-Match with the issue's ETag");
    }

    public static TrackerException stale(long current) {
        return new TrackerException(HttpStatus.PRECONDITION_FAILED, "the issue changed; its version is now " + current);
    }
}
