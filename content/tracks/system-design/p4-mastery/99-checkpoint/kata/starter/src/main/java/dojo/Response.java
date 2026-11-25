package dojo;

/** The result of routing one request; cell and body are null unless it was served. */
public record Response(Outcome outcome, String cell, String body) {}
