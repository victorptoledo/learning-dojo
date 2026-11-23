package dojo.raft;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Predicate;

/**
 * The simulator: it ticks every live node, delivers messages in a fixed order until the
 * network is quiet, applies partitions, drops and crashes, and checks Raft's safety
 * promises after every tick.
 */
final class Cluster {
    static final int HEARTBEAT = 3;

    final List<String> ids = new ArrayList<>();
    final Map<String, RaftNode> nodes = new TreeMap<>();
    final Map<String, MemoryStorage> disks = new HashMap<>();
    final Map<String, RaftConfig> configs = new HashMap<>();
    final Set<String> crashed = new HashSet<>();
    final List<Envelope> delivered = new ArrayList<>();
    private List<Set<String>> groups;
    Predicate<Envelope> drop = e -> false;
    private final Map<Long, String> leaders = new HashMap<>();
    private final Map<Long, Entry> committed = new HashMap<>();

    Cluster(int size) {
        this(size, 0);
    }

    /** Nodes n1..nN; node i times out after 10 + 7(i-1) ticks, so n1 is the first candidate. */
    Cluster(int size, int snapshotEvery) {
        for (int i = 1; i <= size; i++) {
            ids.add("n" + i);
        }
        for (int i = 1; i <= size; i++) {
            String id = "n" + i;
            configs.put(id, new RaftConfig(10 + 7 * (i - 1), HEARTBEAT, snapshotEvery));
            disks.put(id, new MemoryStorage());
            nodes.put(id, new RaftNode(id, ids, configs.get(id), disks.get(id)));
        }
    }

    RaftNode node(String id) {
        return nodes.get(id);
    }

    void tick(int times) {
        for (int t = 0; t < times; t++) {
            for (String id : ids) {
                if (!crashed.contains(id)) {
                    nodes.get(id).tick();
                }
            }
            deliver();
        }
    }

    /** Delivers messages, round after round, until no node has anything to send. */
    void deliver() {
        for (int round = 0; round < 10_000; round++) {
            List<Envelope> batch = new ArrayList<>();
            for (String id : ids) {
                if (!crashed.contains(id)) {
                    batch.addAll(nodes.get(id).drainOutbox());
                }
            }
            if (batch.isEmpty()) {
                check();
                return;
            }
            for (Envelope e : batch) {
                if (!crashed.contains(e.to()) && canTalk(e.from(), e.to()) && !drop.test(e)) {
                    delivered.add(e);
                    nodes.get(e.to()).receive(e);
                }
            }
        }
        fail("the nodes never stop sending messages to each other");
    }

    @SafeVarargs
    final void partition(Set<String>... parts) {
        groups = List.of(parts);
    }

    void isolate(String id) {
        Set<String> rest = new HashSet<>(ids);
        rest.remove(id);
        partition(Set.of(id), rest);
    }

    void heal() {
        groups = null;
    }

    private boolean canTalk(String a, String b) {
        if (groups == null) {
            return true;
        }
        for (Set<String> group : groups) {
            if (group.contains(a)) {
                return group.contains(b);
            }
        }
        return false;
    }

    void crash(String id) {
        nodes.get(id).drainOutbox();
        crashed.add(id);
    }

    /** A new process on the same disk. */
    void restart(String id) {
        nodes.put(id, new RaftNode(id, ids, configs.get(id), disks.get(id)));
        crashed.remove(id);
    }

    /** The live leader with the highest term, or null. */
    RaftNode leader() {
        RaftNode best = null;
        for (String id : ids) {
            RaftNode n = nodes.get(id);
            if (!crashed.contains(id) && n.role() == Role.LEADER && (best == null || n.term() > best.term())) {
                best = n;
            }
        }
        return best;
    }

    String leaderId() {
        for (String id : ids) {
            if (nodes.get(id) == leader()) {
                return id;
            }
        }
        return null;
    }

    /** Ticks until a live leader exists that every live node in its group follows. */
    String awaitLeader(int maxTicks) {
        for (int t = 0; t < maxTicks; t++) {
            String id = leaderId();
            if (id != null && followed(id)) {
                return id;
            }
            tick(1);
        }
        fail("no leader after " + maxTicks + " ticks");
        return null;
    }

    private boolean followed(String leader) {
        for (String id : ids) {
            if (!crashed.contains(id) && canTalk(id, leader) && !leader.equals(nodes.get(id).leader())) {
                return false;
            }
        }
        return true;
    }

    /** Election safety and state machine safety, on every live node. */
    void check() {
        for (String id : ids) {
            if (crashed.contains(id)) {
                continue;
            }
            RaftNode n = nodes.get(id);
            if (n.role() == Role.LEADER) {
                String other = leaders.putIfAbsent(n.term(), id);
                if (other != null && !other.equals(id)) {
                    fail("two leaders in term " + n.term() + ": " + other + " and " + id);
                }
            }
            long base = n.snapshotIndex();
            List<Entry> log = n.log();
            long commit = Math.min(n.commitIndex(), base + log.size());
            for (long index = base + 1; index <= commit; index++) {
                Entry entry = log.get((int) (index - base - 1));
                Entry first = committed.putIfAbsent(index, entry);
                if (first != null && !first.equals(entry)) {
                    fail("index " + index + " was committed as " + first + " but " + id + " has " + entry);
                }
            }
        }
    }

    /** The committed commands from index 1, as the simulator saw them. */
    List<String> committedCommands() {
        List<String> commands = new ArrayList<>();
        for (long i = 1; committed.containsKey(i); i++) {
            commands.add(committed.get(i).command());
        }
        return commands;
    }

    void assertSameLogs() {
        RaftNode lead = leader();
        assertTrue(lead != null, "no leader");
        for (String id : ids) {
            if (!crashed.contains(id)) {
                assertEquals(lead.log(), nodes.get(id).log(), id + " has another log");
                assertEquals(lead.commitIndex(), nodes.get(id).commitIndex(), id + " commit index");
            }
        }
    }
}
