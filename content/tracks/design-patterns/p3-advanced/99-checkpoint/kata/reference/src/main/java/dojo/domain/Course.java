package dojo.domain;

/** A course offering; {@code prerequisite} is a course code, or {@code null} when there is none. */
public record Course(String code, int capacity, int minCredits, String prerequisite) {
    public Course {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("course code is required");
        }
        if (capacity < 1) {
            throw new IllegalArgumentException("capacity must be at least 1");
        }
        if (minCredits < 0) {
            throw new IllegalArgumentException("credits cannot be negative");
        }
    }
}
