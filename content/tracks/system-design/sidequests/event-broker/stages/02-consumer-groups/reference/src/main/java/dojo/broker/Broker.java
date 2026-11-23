package dojo.broker;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.TreeMap;
import java.util.TreeSet;

/** A message broker in one process: partitioned logs, consumer groups. See README.md. */
public final class Broker {

    /** One partition: the kept messages and the offset of the first one. */
    private static final class Partition {
        final ArrayList<Message> kept = new ArrayList<>();
        int head; // kept.get(head) is the message at `start`
        long start;
        long end;

        List<Message> read(long from, int max) {
            int first = head + (int) (from - start);
            int last = Math.min(kept.size(), first + Math.max(0, max));
            return new ArrayList<>(kept.subList(first, last));
        }

        void dropOldest() {
            kept.set(head++, null);
            start++;
            if (head > 1_024 && head * 2 > kept.size()) {
                kept.subList(0, head).clear();
                head = 0;
            }
        }

        Message oldest() {
            return head < kept.size() ? kept.get(head) : null;
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

    /** A consumer group: its members, who owns what, where each owner is, and the commits. */
    private static final class Group {
        final String topic;
        final Map<String, Instant> heartbeats = new HashMap<>();
        final Map<Integer, String> owners = new TreeMap<>();
        final Map<Integer, Long> positions = new HashMap<>();
        final Map<Integer, Long> commits = new HashMap<>();
        int generation;

        Group(String topic) {
            this.topic = topic;
        }
    }

    private final Clock clock;
    private final BrokerConfig config;
    private final Map<String, Topic> topics = new HashMap<>();
    private final Map<String, Group> groups = new HashMap<>();

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
                while (p.oldest() != null && !p.oldest().timestamp().plus(t.retention).isAfter(now)) {
                    p.dropOldest();
                    deleted++;
                }
            }
        }
        return deleted;
    }

    // ---- consumer groups ----

    public void join(String group, String topic, String member) {
        topic(topic);
        Group g = groups.computeIfAbsent(group, name -> new Group(topic));
        if (!g.topic.equals(topic)) {
            throw new IllegalArgumentException("group " + group + " reads " + g.topic + ", not " + topic);
        }
        expire(g);
        if (g.heartbeats.put(member, clock.instant()) == null) {
            rebalance(g);
        }
    }

    public void leave(String group, String member) {
        Group g = group(group);
        if (g.heartbeats.remove(member) != null) {
            rebalance(g);
        }
    }

    public int generation(String group) {
        return group(group).generation;
    }

    public Map<Integer, String> assignment(String group) {
        return new TreeMap<>(group(group).owners);
    }

    public Batch poll(String group, String member, int max) {
        Group g = group(group);
        if (!g.heartbeats.containsKey(member)) {
            throw new IllegalStateException(member + " is not a member of " + group + ": join again");
        }
        g.heartbeats.put(member, clock.instant());
        List<Message> out = new ArrayList<>();
        for (var owned : g.owners.entrySet()) {
            if (!owned.getValue().equals(member) || out.size() >= max) {
                continue;
            }
            int p = owned.getKey();
            List<Message> got = read(g.topic, p, position(g, p), max - out.size());
            out.addAll(got);
            g.positions.put(p, position(g, p) + got.size());
        }
        return new Batch(g.generation, out);
    }

    public void commit(String group, String member, int generation, int partition, long offset) {
        Group g = group(group);
        if (generation != g.generation || !member.equals(g.owners.get(partition))) {
            throw new CommitFailedException(member + " cannot commit partition " + partition
                    + " in generation " + generation + " (now " + g.generation + ")");
        }
        Partition p = partition(g.topic, partition);
        if (offset < 0 || offset > p.end) {
            throw new IllegalArgumentException("offset " + offset + " is outside the partition");
        }
        g.commits.put(partition, offset);
    }

    public long committed(String group, int partition) {
        Group g = groups.get(group);
        if (g == null) {
            return -1;
        }
        expire(g);
        return g.commits.getOrDefault(partition, -1L);
    }

    /** Where the owner of a partition reads next; set from the commit after a rebalance. */
    private long position(Group g, int partition) {
        Partition p = partition(g.topic, partition);
        long at = g.positions.getOrDefault(partition, p.start);
        return Math.max(at, p.start);
    }

    private void expire(Group g) {
        var now = clock.instant();
        boolean removed = g.heartbeats.values()
                .removeIf(last -> !last.plus(config.sessionTimeout()).isAfter(now));
        if (removed) {
            rebalance(g);
        }
    }

    /** A new generation: range assignment over the sorted members, positions back to the commits. */
    private void rebalance(Group g) {
        g.generation++;
        g.owners.clear();
        g.positions.clear();
        List<String> members = new ArrayList<>(new TreeSet<>(g.heartbeats.keySet()));
        int partitions = topic(g.topic).partitions.length;
        if (!members.isEmpty()) {
            int next = 0;
            for (int i = 0; i < members.size(); i++) {
                int count = partitions / members.size() + (i < partitions % members.size() ? 1 : 0);
                for (int c = 0; c < count; c++) {
                    g.owners.put(next++, members.get(i));
                }
            }
        }
        for (int p = 0; p < partitions; p++) {
            Partition part = partition(g.topic, p);
            long committed = g.commits.getOrDefault(p, part.start);
            g.positions.put(p, Math.max(part.start, committed));
        }
    }

    private Group group(String name) {
        Group g = groups.get(name);
        if (g == null) {
            throw new NoSuchElementException("no group " + name);
        }
        expire(g);
        return g;
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
