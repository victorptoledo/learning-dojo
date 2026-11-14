package dojo;

/** An article; {@code version} grows by one on every change. */
public record Article(long id, String title, String body, int version) {}
