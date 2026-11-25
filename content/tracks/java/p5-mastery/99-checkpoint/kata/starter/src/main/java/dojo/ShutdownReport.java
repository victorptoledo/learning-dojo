package dojo;

/** What happened to the accepted orders: stored + failed + abandoned == accepted. Already written. */
public record ShutdownReport(long stored, long failed, long abandoned) {}
