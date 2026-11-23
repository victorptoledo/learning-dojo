package dojo.raft;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class Stage5SnapshotTest {
    private static final int EVERY = 10;

    private final Cluster cluster = new Cluster(3, EVERY);
    private final Map<String, String> expected = new HashMap<>();
    private long sequence;

    private RaftNode leader() {
        return cluster.node(cluster.awaitLeader(60));
    }

    private void write(RaftNode leader, int count) {
        for (int i = 0; i < count; i++) {
            sequence++;
            String key = "k" + (sequence % 7);
            leader.submit("alice", sequence, "set " + key + " v" + sequence);
            expected.put(key, "v" + sequence);
            cluster.tick(1);
        }
        cluster.tick(2 * Cluster.HEARTBEAT);
    }

    @Test
    void nodesCompactTheirLogsAndKeepAbsoluteIndexes() {
        RaftNode leader = leader();
        write(leader, 25);
        for (String id : cluster.ids) {
            RaftNode n = cluster.node(id);
            assertEquals(20, n.snapshotIndex(), id);
            assertEquals(25, n.lastIndex(), id);
            assertEquals(5, n.log().size(), id + " keeps only the entries after the snapshot");
            assertEquals(expected, n.data(), id);
        }
        assertEquals(26, leader.submit("alice", ++sequence, "set k0 last"));
        MemoryStorage disk = cluster.disks.get("n2");
        assertEquals(20, disk.load().snapshot().index());
        assertEquals(1, disk.load().snapshot().term());
        assertEquals(Map.of("alice", 20L), disk.load().snapshot().clients());
    }

    @Test
    void aRestartedNodeStartsFromItsSnapshot() {
        RaftNode leader = leader();
        write(leader, 23);
        cluster.crash("n3");
        cluster.restart("n3");
        RaftNode n3 = cluster.node("n3");
        assertEquals(20, n3.snapshotIndex());
        assertEquals(20, n3.lastApplied());
        assertEquals(20, n3.commitIndex());
        assertEquals(23, n3.lastIndex());
        assertEquals(cluster.disks.get("n3").load().snapshot().data(), n3.data());
        cluster.tick(Cluster.HEARTBEAT);
        assertEquals(expected, n3.data(), "the rest comes from the log once the leader says it is committed");
    }

    @Test
    void aFollowerFarBehindGetsTheSnapshot() {
        RaftNode leader = leader();
        cluster.isolate("n3");
        write(leader, 45);
        assertEquals(40, leader.snapshotIndex());
        assertEquals(0, cluster.node("n3").lastIndex());
        cluster.heal(); // n3 timed out alone with higher terms: a new election comes first
        RaftNode now = leader();
        cluster.tick(2 * Cluster.HEARTBEAT);
        RaftNode n3 = cluster.node("n3");
        assertTrue(cluster.delivered.stream().anyMatch(e -> e.to().equals("n3") && e.msg() instanceof Msg.InstallSnapshot),
                "the entries n3 needs are only in the snapshot");
        assertEquals(expected, n3.data());
        assertEquals(45, n3.lastIndex());
        assertEquals(45, n3.commitIndex());
        assertEquals(40, n3.snapshotIndex());
        assertEquals(now.log(), n3.log());
        write(now, 3);
        assertEquals(expected, n3.data());
    }

    @Test
    void installingASnapshotKeepsTheEntriesAfterIt() {
        RaftNode n2 = new RaftNode("n2", List.of("n1", "n2", "n3"), new RaftConfig(10, 3, 0), new MemoryStorage());
        n2.receive(new Envelope("n1", "n2", new Msg.AppendEntries(1, "n1", 0, 0,
                List.of(new Entry(1, "alice 1 set a 1"), new Entry(1, "alice 2 set b 2"), new Entry(1, "alice 3 set c 3")), 0)));
        n2.drainOutbox();
        var snapshot = new Snapshot(2, 1, Map.of("a", "1", "b", "2"), Map.of("alice", 2L));
        n2.receive(new Envelope("n1", "n2", new Msg.InstallSnapshot(1, "n1", snapshot)));
        assertEquals(List.of(new Envelope("n2", "n1", new Msg.AppendReply(1, "n2", true, 2))), n2.drainOutbox());
        assertEquals(2, n2.snapshotIndex());
        assertEquals(List.of(new Entry(1, "alice 3 set c 3")), n2.log(), "the entry after the snapshot matches: kept");
        assertEquals(Map.of("a", "1", "b", "2"), n2.data());
        assertEquals(2, n2.commitIndex());
        var other = new Snapshot(3, 2, Map.of("z", "9"), Map.of("bob", 1L));
        n2.receive(new Envelope("n3", "n2", new Msg.InstallSnapshot(2, "n3", other)));
        assertEquals(List.of(), n2.log(), "entry 3 has another term in this snapshot: the log goes");
        assertEquals(Map.of("z", "9"), n2.data());
        assertEquals(3, n2.lastIndex());
        assertEquals("n3", n2.leader());
    }

    @Test
    void clientSequencesSurviveInTheSnapshot() {
        RaftNode leader = leader();
        write(leader, 25);
        for (String id : cluster.ids) {
            cluster.crash(id);
        }
        for (String id : cluster.ids) {
            cluster.restart(id);
        }
        RaftNode next = leader();
        next.submit("alice", 3, "set k3 rollback"); // a retry from long ago
        next.submit("bob", 1, "set fresh yes");
        expected.put("fresh", "yes");
        cluster.tick(2 * Cluster.HEARTBEAT);
        for (String id : cluster.ids) {
            assertEquals(expected, cluster.node(id).data(), id);
        }
    }
}
