package dojo;

import java.time.Instant;

/** A document. The version goes up by one on every change; updatedAt has whole seconds. */
public record Document(String id, String title, String body, long version, Instant updatedAt) {}
