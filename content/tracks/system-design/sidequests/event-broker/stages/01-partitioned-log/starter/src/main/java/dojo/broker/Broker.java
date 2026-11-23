package dojo.broker;

import java.time.Clock;
import java.time.Duration;
import java.util.List;

/** A message broker in one process. See README.md. */
public final class Broker {

    public Broker(Clock clock, BrokerConfig config) {
    }

    public BrokerConfig config() {
        throw new UnsupportedOperationException("todo: config");
    }

    public void createTopic(String topic, int partitions, Duration retention) {
        throw new UnsupportedOperationException("todo: createTopic");
    }

    public int partitions(String topic) {
        throw new UnsupportedOperationException("todo: partitions");
    }

    public Sent send(String topic, String key, String value) {
        throw new UnsupportedOperationException("todo: send");
    }

    public List<Message> read(String topic, int partition, long fromOffset, int max) {
        throw new UnsupportedOperationException("todo: read");
    }

    public long startOffset(String topic, int partition) {
        throw new UnsupportedOperationException("todo: startOffset");
    }

    public long endOffset(String topic, int partition) {
        throw new UnsupportedOperationException("todo: endOffset");
    }

    public int enforceRetention() {
        throw new UnsupportedOperationException("todo: enforceRetention");
    }
}
