package dojo;

import java.time.Instant;

/** A post: ids grow with every post, {@code at} comes from the service's clock. */
public record Post(long id, String author, Instant at) {}
