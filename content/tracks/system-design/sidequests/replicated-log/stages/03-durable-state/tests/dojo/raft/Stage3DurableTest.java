package dojo.raft;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

class Stage3DurableTest {
    private static final List<String> THREE = List.of("n1", "n2", "n3");
    private static final RaftConfig CONFIG = new RaftConfig(10, 3, 0);

    @Test
    void aVoteSurvivesARestart() {
        var disk = new MemoryStorage();
        RaftNode n2 = new RaftNode("n2", THREE, CONFIG, disk);
        n2.receive(new Envelope("n1", "n2", new Msg.RequestVote(4, "n1", 0, 0)));
        assertEquals(new Msg.Vote(4, "n2", true), n2.drainOutbox().get(0).msg());
        assertEquals(4, disk.load().term(), "saved before the vote went out");
        assertEquals("n1", disk.load().votedFor());
        RaftNode again = new RaftNode("n2", THREE, CONFIG, disk);
        assertEquals(4, again.term());
        assertEquals("n1", again.votedFor());
        assertEquals(Role.FOLLOWER, again.role());
        again.receive(new Envelope("n3", "n2", new Msg.RequestVote(4, "n3", 0, 0)));
        assertEquals(new Msg.Vote(4, "n2", false), again.drainOutbox().get(0).msg(),
                "a restarted node never votes twice in one term");
    }

    @Test
    void aCandidateSavesItsOwnVote() {
        var disk = new MemoryStorage();
        RaftNode n1 = new RaftNode("n1", THREE, CONFIG, disk);
        for (int i = 0; i < 10; i++) {
            n1.tick();
        }
        assertEquals(1, disk.load().term());
        assertEquals("n1", disk.load().votedFor());
    }

    @Test
    void theLogSurvivesAndTheRestIsVolatile() {
        var disk = new MemoryStorage();
        RaftNode n2 = new RaftNode("n2", THREE, CONFIG, disk);
        n2.receive(new Envelope("n1", "n2", new Msg.AppendEntries(3, "n1", 0, 0,
                List.of(new Entry(2, "a"), new Entry(3, "b")), 2)));
        assertEquals(List.of(new Entry(2, "a"), new Entry(3, "b")), disk.load().log());
        assertEquals(Snapshot.EMPTY, disk.load().snapshot());
        assertNull(disk.load().votedFor());
        RaftNode again = new RaftNode("n2", THREE, CONFIG, disk);
        assertEquals(List.of(new Entry(2, "a"), new Entry(3, "b")), again.log());
        assertEquals(2, again.lastIndex());
        assertEquals(0, again.commitIndex(), "the commit index is learned again from the leader");
        assertNull(again.leader());
        assertEquals(3, again.term());
    }

    @Test
    void restartedFollowersKeepWhatTheyHad() {
        var cluster = new Cluster(3);
        RaftNode leader = cluster.node(cluster.awaitLeader(20));
        for (int i = 0; i < 10; i++) {
            leader.propose("cmd-" + i);
        }
        cluster.tick(Cluster.HEARTBEAT);
        cluster.crash("n2");
        cluster.crash("n3");
        cluster.restart("n2");
        cluster.restart("n3");
        assertEquals(10, cluster.node("n2").lastIndex());
        cluster.tick(Cluster.HEARTBEAT);
        cluster.assertSameLogs();
        assertEquals(10, cluster.node("n3").commitIndex());
    }

    @Test
    void theWholeClusterRestartsWithoutLosingCommits() {
        var cluster = new Cluster(3);
        RaftNode leader = cluster.node(cluster.awaitLeader(20));
        for (int i = 0; i < 5; i++) {
            leader.propose("cmd-" + i);
        }
        cluster.tick(Cluster.HEARTBEAT);
        for (String id : cluster.ids) {
            cluster.crash(id);
        }
        for (String id : cluster.ids) {
            cluster.restart(id);
        }
        RaftNode next = cluster.node(cluster.awaitLeader(50));
        assertTrue(next.term() > 1);
        next.propose("after");
        cluster.tick(Cluster.HEARTBEAT);
        cluster.assertSameLogs();
        assertEquals(6, next.commitIndex(), "the new entry commits the old ones with it");
        assertEquals(List.of("cmd-0", "cmd-1", "cmd-2", "cmd-3", "cmd-4", "after"), cluster.committedCommands());
    }

    @Test
    void aNodeThatFellBehindCannotBecomeLeader() {
        var cluster = new Cluster(3);
        RaftNode leader = cluster.node(cluster.awaitLeader(20));
        cluster.isolate("n2"); // n2 has the shortest timeout after n1
        for (int i = 0; i < 5; i++) {
            leader.propose("cmd-" + i);
        }
        cluster.tick(Cluster.HEARTBEAT);
        assertEquals(5, leader.commitIndex(), "committed by n1 and n3");
        cluster.crash("n1");
        cluster.heal();
        cluster.tick(120);
        assertEquals("n3", cluster.leaderId(), "n2 asks first, but n3 refuses: n2's log is behind");
        assertEquals(5, cluster.node("n2").lastIndex(), "n2 caught up from n3");
        cluster.restart("n1");
        cluster.tick(2 * Cluster.HEARTBEAT);
        cluster.assertSameLogs();
    }

    @Test
    void aRestartedLeaderComesBackAsAFollower() {
        var cluster = new Cluster(3);
        cluster.awaitLeader(20);
        cluster.crash("n1");
        String next = cluster.awaitLeader(60);
        assertNotEquals("n1", next);
        cluster.restart("n1");
        assertEquals(Role.FOLLOWER, cluster.node("n1").role());
        cluster.tick(Cluster.HEARTBEAT);
        assertEquals(next, cluster.node("n1").leader());
    }

    @Test
    void chaosNeverBreaksSafety() {
        var cluster = new Cluster(5);
        var random = new Random(20261123);
        List<String> proposed = new ArrayList<>();
        for (int step = 0; step < 600; step++) {
            int roll = random.nextInt(100);
            String id = cluster.ids.get(random.nextInt(5));
            if (roll < 3 && !cluster.crashed.contains(id) && cluster.crashed.size() < 2) {
                cluster.crash(id);
            } else if (roll < 8 && cluster.crashed.contains(id)) {
                cluster.restart(id);
            } else if (roll < 11) {
                cluster.isolate(id);
            } else if (roll < 16) {
                cluster.heal();
            }
            RaftNode leader = cluster.leader();
            if (leader != null && random.nextInt(3) == 0) {
                String command = "cmd-" + step;
                leader.propose(command);
                proposed.add(command);
            }
            cluster.tick(1); // the cluster checks both safety rules after every tick
        }
        cluster.heal();
        for (String id : List.copyOf(cluster.crashed)) {
            cluster.restart(id);
        }
        RaftNode leader = cluster.node(cluster.awaitLeader(200));
        leader.propose("final");
        cluster.tick(4 * Cluster.HEARTBEAT);
        cluster.assertSameLogs();
        List<String> committed = cluster.committedCommands();
        assertEquals("final", committed.get(committed.size() - 1));
        assertTrue(committed.size() > 50, "the cluster made progress: " + committed.size());
        assertTrue(proposed.containsAll(committed.subList(0, committed.size() - 1)));
    }
}
