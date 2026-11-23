package dojo.shortener;

/** What the read cache did: resolves answered by it, resolves that read the store, and entries pushed out. */
public record CacheStats(long hits, long misses, long evictions) {
}
