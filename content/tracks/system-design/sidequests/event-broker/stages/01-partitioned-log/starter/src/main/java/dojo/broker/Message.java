package dojo.broker;

import java.time.Instant;

/** One message of a partition; `key` may be null. */
public record Message(String topic, int partition, long offset, String key, String value, Instant timestamp) {
}
