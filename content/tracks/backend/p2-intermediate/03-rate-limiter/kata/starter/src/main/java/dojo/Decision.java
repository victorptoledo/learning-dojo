package dojo;

import java.time.Duration;

/** The answer for one request: allowed or not, what is left, and when to retry (ZERO when allowed). */
public record Decision(boolean allowed, long remaining, Duration retryAfter) {}
