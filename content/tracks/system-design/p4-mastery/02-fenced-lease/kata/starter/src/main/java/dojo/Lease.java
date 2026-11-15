package dojo;

import java.time.Instant;

/** A lease: {@code owner} may act until {@code expiresAt}, and stamps its writes with {@code token}. */
public record Lease(String owner, long token, Instant expiresAt) {}
