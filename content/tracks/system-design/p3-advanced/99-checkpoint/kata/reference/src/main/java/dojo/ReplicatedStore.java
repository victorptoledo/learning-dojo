package dojo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * A partitioned key-value store: one leader per partition, asynchronous followers that consume
 * the leader's log, session guarantees on reads and failover that knows what it lost.
 */
public final class ReplicatedStore {
    private final List<Group> groups = new ArrayList<>();
    private final Map<String, Integer> partitionOfNode = new HashMap<>();
    private long truncated;

    /** The nodes of one partition and the history of its epochs. */
    private static final class Group {
        final List<Node> nodes;
        Node leader;
        /** starts.get(e - 1) is the log position where epoch e began. */
        final List<Long> starts = new ArrayList<>(List.of(0L));

        Group(List<Node> nodes) {
            this.nodes = List.copyOf(nodes);
            this.leader = nodes.get(0);
        }

        long epoch() {
            return starts.size();
        }

        /** The highest position of an epoch that is still part of the history. */
        long survived(long epoch) {
            long survived = Long.MAX_VALUE;
            for (long later = epoch + 1; later <= epoch(); later++) {
                survived = Math.min(survived, starts.get((int) later - 1));
            }
            return survived;
        }

        boolean usable(Node node) {
            return node.position() <= survived(node.lastEpoch());
        }
    }

    public ReplicatedStore(List<List<Node>> partitions) {
        if (partitions.isEmpty()) {
            throw new IllegalArgumentException("at least one partition is required");
        }
        Set<String> names = new HashSet<>();
        for (List<Node> nodes : partitions) {
            if (nodes.isEmpty()) {
                throw new IllegalArgumentException("every partition needs at least one node");
            }
            for (Node node : nodes) {
                if (!names.add(node.name())) {
                    throw new IllegalArgumentException("node " + node.name() + " appears twice");
                }
                partitionOfNode.put(node.name(), groups.size());
            }
            groups.add(new Group(nodes));
        }
    }

    public int partitionFor(String key) {
        return Math.floorMod(Hashing.hash64(key), groups.size());
    }

    public String leaderOf(int partition) {
        return group(partition).leader.name();
    }

    public long epochOf(int partition) {
        return group(partition).epoch();
    }

    public Session session() {
        return new Session();
    }

    public long put(Session session, String key, String value) {
        int partition = partitionFor(key);
        Group group = groups.get(partition);
        seen(session, partition, group);
        if (group.leader.isDown()) {
            throw new LeaderUnavailableException(partition);
        }
        LogEntry entry = group.leader.append(group.epoch(), key, value);
        session.advance(partition, new Token(group.epoch(), entry.position()));
        return entry.position();
    }

    public Optional<String> get(Session session, String key) {
        int partition = partitionFor(key);
        Group group = groups.get(partition);
        long seen = seen(session, partition, group);
        Node chosen = null;
        for (Node node : group.nodes) {
            if (node != group.leader && !node.isDown() && group.usable(node) && node.position() >= seen) {
                chosen = node;
                break;
            }
        }
        if (chosen == null) {
            if (group.leader.isDown()) {
                throw new LeaderUnavailableException(partition);
            }
            chosen = group.leader;
        }
        Optional<String> value = chosen.get(key);
        session.advance(partition, new Token(group.epoch(), Math.max(seen, chosen.position())));
        return value;
    }

    public int replicate(int maxEntries) {
        if (maxEntries < 1) {
            throw new IllegalArgumentException("maxEntries must be at least 1");
        }
        int shipped = 0;
        for (Group group : groups) {
            if (group.leader.isDown()) {
                continue;
            }
            for (Node node : group.nodes) {
                if (node == group.leader || node.isDown()) {
                    continue;
                }
                if (!group.usable(node)) {
                    truncated += node.truncateTo(group.survived(node.lastEpoch()));
                }
                for (LogEntry entry : group.leader.entriesAfter(node.position(), maxEntries)) {
                    node.apply(entry);
                    shipped++;
                }
            }
        }
        return shipped;
    }

    public long lag(String node) {
        Integer partition = partitionOfNode.get(node);
        if (partition == null) {
            throw new IllegalArgumentException("unknown node: " + node);
        }
        Group group = groups.get(partition);
        for (Node candidate : group.nodes) {
            if (candidate.name().equals(node)) {
                return Math.max(0, group.leader.position() - candidate.position());
            }
        }
        throw new IllegalStateException("unreachable");
    }

    public Failover failover(int partition) {
        Group group = group(partition);
        if (!group.leader.isDown()) {
            throw new IllegalStateException("the leader of partition " + partition + " is up");
        }
        Node best = null;
        for (Node node : group.nodes) {
            if (node != group.leader && !node.isDown() && group.usable(node)
                    && (best == null || node.position() > best.position())) {
                best = node;
            }
        }
        if (best == null) {
            throw new LeaderUnavailableException(partition);
        }
        Node old = group.leader;
        long lost = Math.max(0, old.position() - best.position());
        group.starts.add(best.position());
        group.leader = best;
        return new Failover(partition, old.name(), best.name(), group.epoch(), lost);
    }

    public long truncatedEntries() {
        return truncated;
    }

    /** The position the session has seen in this partition, or LostWriteException if it was lost. */
    private static long seen(Session session, int partition, Group group) {
        Optional<Token> token = session.token(partition);
        if (token.isEmpty()) {
            return 0;
        }
        long epoch = token.get().epoch();
        long position = token.get().position();
        if (epoch < group.epoch() && position > group.survived(epoch)) {
            throw new LostWriteException(partition);
        }
        return position;
    }

    private Group group(int partition) {
        if (partition < 0 || partition >= groups.size()) {
            throw new IllegalArgumentException("no partition " + partition);
        }
        return groups.get(partition);
    }
}
