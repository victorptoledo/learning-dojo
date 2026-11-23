package dojo.broker;

/** Where a sent message was written. */
public record Sent(int partition, long offset) {
}
