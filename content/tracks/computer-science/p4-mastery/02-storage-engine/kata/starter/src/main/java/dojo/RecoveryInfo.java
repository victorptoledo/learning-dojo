package dojo;

/** What opening the store found in the log: valid batches replayed, and bytes of a damaged tail dropped. */
public record RecoveryInfo(int batchesReplayed, long bytesDiscarded) {}
