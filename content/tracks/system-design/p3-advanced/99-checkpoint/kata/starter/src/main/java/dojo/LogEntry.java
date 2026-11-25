package dojo;

/** One write in a partition's replication log: positions are 1, 2, 3, ... */
public record LogEntry(long epoch, long position, String key, String value) {}
