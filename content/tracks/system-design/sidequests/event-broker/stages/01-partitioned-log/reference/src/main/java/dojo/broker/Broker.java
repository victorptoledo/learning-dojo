package dojo.broker;

import java.time.Clock;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/** A message broker in one process: partitioned logs with retention. See README.md. */
public final class Broker {

    /** One partition: the kept messages and the offset of the first one. */
    private static final class Partition {
        final ArrayDeque<Message> kept = new ArrayDeque<>();
        long start;
        long end;

        List<Message> read(long from, int max) {
            List<Message> out = new ArrayList<>();
            int skip = (int) (from - start);
            for (Message m : kept) {
                if (skip-- > 0) {
                    continue;
                }
                if (out.size() == max) {
                    break;
                }
                out.add(m);
            }
            return out;
        }
    }

    private static final class Topic {
        final Partition[] partitions;
        final Duration retention;
        int nextRoundRobin;

        Topic(int count, Duration retention) {
            partitions = new Partition[count];
            for (int i = 0; i < count; i++) {
                partitions[i] = new Partition();
            }
            this.retention = retention;
        }
    }

    private final Clock clock;
    private final BrokerConfig config;
    private final Map<String, Topic> topics = new HashMap<>();

    public Broker(Clock clock, BrokerConfig config) {
        this.clock = clock;
        this.config = config;
    }

    public BrokerConfig config() {
        return config;
    }

    public void createTopic(String topic, int partitions, Duration retention) {
        if (topic == null || topic.isBlank() || topics.containsKey(topic)) {
            throw new IllegalArgumentException("a topic needs a new, non-blank name: " + topic);
        }
        if (partitions < 1 || retention == null || retention.isNegative() || retention.isZero()) {
            throw new IllegalArgumentException("a topic needs partitions and a positive retention");
        }
        topics.put(topic, new Topic(partitions, retention));
    }

    public int partitions(String topic) {
        return topic(topic).partitions.length;
    }

    public Sent send(String topic, String key, String value) {
        Topic t = topic(topic);
        int p;
        if (key == null) {
            p = t.nextRoundRobin;
            t.nextRoundRobin = (p + 1) % t.partitions.length;
        } else {
            p = Math.floorMod(Hashing.hash64(key), t.partitions.length);
        }
        Partition partition = t.partitions[p];
        long offset = partition.end++;
        partition.kept.add(new Message(topic, p, offset, key, value, clock.instant()));
        return new Sent(p, offset);
    }

    public List<Message> read(String topic, int partition, long fromOffset, int max) {
        Partition p = partition(topic, partition);
        if (fromOffset < p.start || fromOffset > p.end) {
            throw new OffsetOutOfRangeException(
                    "offset " + fromOffset + " is outside " + p.start + ".." + p.end);
        }
        return p.read(fromOffset, max);
    }

    public long startOffset(String topic, int partition) {
        return partition(topic, partition).start;
    }

    public long endOffset(String topic, int partition) {
        return partition(topic, partition).end;
    }

    public int enforceRetention() {
        var now = clock.instant();
        int deleted = 0;
        for (Topic t : topics.values()) {
            for (Partition p : t.partitions) {
                while (!p.kept.isEmpty() && !p.kept.peek().timestamp().plus(t.retention).isAfter(now)) {
                    p.kept.poll();
                    p.start++;
                    deleted++;
                }
            }
        }
        return deleted;
    }

    private Topic topic(String name) {
        Topic t = topics.get(name);
        if (t == null) {
            throw new NoSuchElementException("no topic " + name);
        }
        return t;
    }

    private Partition partition(String topic, int partition) {
        Topic t = topic(topic);
        if (partition < 0 || partition >= t.partitions.length) {
            throw new NoSuchElementException("topic " + topic + " has no partition " + partition);
        }
        return t.partitions[partition];
    }
}
