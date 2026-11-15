package dojo;

/** Counters since the cache was created. */
public record CacheStats(long hits, long misses, long evictions, long expirations) {}
