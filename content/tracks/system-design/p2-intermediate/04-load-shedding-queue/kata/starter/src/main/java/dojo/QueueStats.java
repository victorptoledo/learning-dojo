package dojo;

/** Counters since the queue was created. */
public record QueueStats(long accepted, long rejected, long shed, long expired, long served) {}
