package dojo;

/** What the service has done so far. Already written. */
public record Metrics(long accepted, long rejected, long stored, long failed,
        long cacheHits, long cacheMisses, int cacheSize) {}
