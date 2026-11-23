package dojo.raft;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class Stage1ElectionTest {
    private static final List<String> THREE = List.of("n1", "n2", "n3");
    private static final RaftConfig CONFIG = new RaftConfig(10, 3, 0);

    private static RaftNode node(String id) {
        return new RaftNode(id, THREE, CONFIG, new MemoryStorage());
    }

    private static void tick(RaftNode node, int times) {
        for (int i = 0; i < times; i++) {
            node.tick();
        }
    }

    private static Envelope from(String sender, String to, Msg msg) {
        return new Envelope(sender, to, msg);
    }

    // ---- one node at a time ----

    @Test
    void aFollowerStartsAnElectionWhenItsTimeoutEnds() {
        RaftNode n1 = node("n1");
        tick(n1, 9);
        assertEquals(Role.FOLLOWER, n1.role());
        assertEquals(List.of(), n1.drainOutbox());
        tick(n1, 1);
        assertEquals(Role.CANDIDATE, n1.role());
        assertEquals(1, n1.term());
        assertEquals("n1", n1.votedFor());
        assertEquals(List.of(
                from("n1", "n2", new Msg.RequestVote(1, "n1", 0, 0)),
                from("n1", "n3", new Msg.RequestVote(1, "n1", 0, 0))), n1.drainOutbox());
        assertEquals(List.of(), n1.drainOutbox(), "draining empties the outbox");
    }

    @Test
    void aCandidateThatDoesNotWinTriesAgainInANewTerm() {
        RaftNode n1 = node("n1");
        tick(n1, 10);
        n1.drainOutbox();
        tick(n1, 10);
        assertEquals(2, n1.term());
        assertEquals(Role.CANDIDATE, n1.role());
        assertEquals(2, n1.drainOutbox().size());
    }

    @Test
    void aMajorityOfVotesMakesALeaderThatSendsHeartbeats() {
        RaftNode n1 = node("n1");
        tick(n1, 10);
        n1.drainOutbox();
        n1.receive(from("n2", "n1", new Msg.Vote(1, "n2", true)));
        assertEquals(Role.LEADER, n1.role());
        assertEquals("n1", n1.leader());
        List<Envelope> beats = List.of(
                from("n1", "n2", new Msg.AppendEntries(1, "n1", 0, 0, List.of(), 0)),
                from("n1", "n3", new Msg.AppendEntries(1, "n1", 0, 0, List.of(), 0)));
        assertEquals(beats, n1.drainOutbox(), "a new leader announces itself at once");
        n1.receive(from("n3", "n1", new Msg.Vote(1, "n3", true)));
        tick(n1, 2);
        assertEquals(List.of(), n1.drainOutbox());
        tick(n1, 1);
        assertEquals(beats, n1.drainOutbox(), "then every heartbeat interval");
        tick(n1, 30);
        assertEquals(Role.LEADER, n1.role(), "a leader never times out");
        assertEquals(1, n1.term());
    }

    @Test
    void refusedVotesAndOldVotesDoNotCount() {
        RaftNode n1 = node("n1");
        tick(n1, 10);
        n1.receive(from("n2", "n1", new Msg.Vote(1, "n2", false)));
        assertEquals(Role.CANDIDATE, n1.role());
        tick(n1, 10); // term 2
        n1.receive(from("n3", "n1", new Msg.Vote(1, "n3", true)));
        assertEquals(Role.CANDIDATE, n1.role(), "a vote of term 1 means nothing in term 2");
    }

    @Test
    void aNodeVotesOncePerTerm() {
        RaftNode n2 = node("n2");
        n2.receive(from("n1", "n2", new Msg.RequestVote(1, "n1", 0, 0)));
        assertEquals(List.of(from("n2", "n1", new Msg.Vote(1, "n2", true))), n2.drainOutbox());
        assertEquals("n1", n2.votedFor());
        n2.receive(from("n3", "n2", new Msg.RequestVote(1, "n3", 0, 0)));
        assertEquals(List.of(from("n2", "n3", new Msg.Vote(1, "n2", false))), n2.drainOutbox());
        n2.receive(from("n1", "n2", new Msg.RequestVote(1, "n1", 0, 0)));
        assertEquals(List.of(from("n2", "n1", new Msg.Vote(1, "n2", true))), n2.drainOutbox(),
                "asking again (a lost answer) gets the same vote");
        n2.receive(from("n3", "n2", new Msg.RequestVote(2, "n3", 0, 0)));
        assertEquals(List.of(from("n2", "n3", new Msg.Vote(2, "n2", true))), n2.drainOutbox(), "a new term, a new vote");
        n2.receive(from("n1", "n2", new Msg.RequestVote(1, "n1", 0, 0)));
        assertEquals(List.of(from("n2", "n1", new Msg.Vote(2, "n2", false))), n2.drainOutbox(),
                "an old term gets the current term back");
    }

    @Test
    void grantingAVoteResetsTheTimer() {
        RaftNode n2 = node("n2");
        tick(n2, 9);
        n2.receive(from("n1", "n2", new Msg.RequestVote(1, "n1", 0, 0)));
        tick(n2, 9);
        assertEquals(Role.FOLLOWER, n2.role());
        tick(n2, 1);
        assertEquals(Role.CANDIDATE, n2.role());
        assertEquals(2, n2.term());
    }

    @Test
    void heartbeatsKeepAFollowerAndAHigherTermStopsACandidate() {
        RaftNode n2 = node("n2");
        for (int i = 0; i < 10; i++) {
            tick(n2, 8);
            n2.receive(from("n1", "n2", new Msg.AppendEntries(0, "n1", 0, 0, List.of(), 0)));
        }
        assertEquals(Role.FOLLOWER, n2.role(), "even a term-0 leader keeps it a follower");
        tick(n2, 10);
        assertEquals(Role.CANDIDATE, n2.role());
        assertEquals(1, n2.term());
        n2.drainOutbox();
        n2.receive(from("n3", "n2", new Msg.AppendEntries(5, "n3", 0, 0, List.of(), 0)));
        assertEquals(Role.FOLLOWER, n2.role());
        assertEquals(5, n2.term());
        assertEquals("n3", n2.leader());
        assertNull(n2.votedFor());
        assertEquals(List.of(from("n2", "n3", new Msg.AppendReply(5, "n2", true, 0))), n2.drainOutbox());
    }

    @Test
    void aMessageFromAnOldTermIsAnsweredWithTheNewOne() {
        RaftNode n2 = node("n2");
        n2.receive(from("n3", "n2", new Msg.AppendEntries(4, "n3", 0, 0, List.of(), 0)));
        n2.drainOutbox();
        n2.receive(from("n1", "n2", new Msg.AppendEntries(3, "n1", 0, 0, List.of(), 0)));
        assertEquals(List.of(from("n2", "n1", new Msg.AppendReply(4, "n2", false, 0))), n2.drainOutbox());
        assertEquals("n3", n2.leader());
    }

    @Test
    void aClusterOfOneElectsItself() {
        RaftNode solo = new RaftNode("solo", List.of("solo"), CONFIG, new MemoryStorage());
        tick(solo, 10);
        assertEquals(Role.LEADER, solo.role());
        assertEquals(1, solo.term());
        assertEquals(List.of(), solo.drainOutbox());
    }

    // ---- a simulated cluster ----

    @Test
    void theFirstNodeToTimeOutBecomesTheLeader() {
        var cluster = new Cluster(3);
        cluster.tick(9);
        assertNull(cluster.leader());
        cluster.tick(1);
        assertEquals("n1", cluster.awaitLeader(1));
        for (String id : List.of("n2", "n3")) {
            assertEquals("n1", cluster.node(id).leader());
            assertEquals("n1", cluster.node(id).votedFor());
            assertEquals(1, cluster.node(id).term());
        }
        cluster.tick(300);
        assertEquals("n1", cluster.leaderId(), "heartbeats keep the leader");
        assertEquals(1, cluster.node("n3").term(), "and nobody starts an election");
    }

    @Test
    void anIsolatedLeaderIsReplacedAndStepsDownWhenItComesBack() {
        var cluster = new Cluster(3);
        cluster.awaitLeader(20);
        cluster.isolate("n1");
        cluster.tick(30);
        assertEquals(Role.LEADER, cluster.node("n2").role());
        assertEquals(2, cluster.node("n2").term());
        assertEquals(Role.LEADER, cluster.node("n1").role(), "alone, n1 still thinks it leads term 1");
        cluster.heal();
        cluster.tick(Cluster.HEARTBEAT);
        assertEquals(Role.FOLLOWER, cluster.node("n1").role());
        assertEquals(2, cluster.node("n1").term());
        assertEquals("n2", cluster.node("n1").leader());
    }

    @Test
    void aMinorityNeverElectsALeader() {
        var cluster = new Cluster(5);
        cluster.partition(Set.of("n1", "n2"), Set.of("n3", "n4", "n5"));
        cluster.tick(200);
        assertEquals(Role.LEADER, cluster.node("n3").role(), "the majority side elects the first of its nodes");
        for (String id : List.of("n1", "n2")) {
            assertTrue(cluster.node(id).role() != Role.LEADER, id + " is in the minority");
        }
        assertTrue(cluster.node("n1").term() > 5, "n1 keeps trying: " + cluster.node("n1").term());
        cluster.heal();
        String leader = cluster.awaitLeader(100);
        cluster.tick(100);
        assertEquals(leader, cluster.leaderId());
    }

    @Test
    void lostMessagesOnlyDelayTheElection() {
        var cluster = new Cluster(3);
        cluster.drop = e -> e.msg() instanceof Msg.Vote && e.msg().term() < 3;
        String leader = cluster.awaitLeader(200);
        assertTrue(cluster.node(leader).term() >= 3);
    }
}
