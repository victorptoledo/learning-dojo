package dojo;

/** The body of PUT /todos/{id}: the new title and state. */
public record TodoUpdate(String title, boolean done) {}
