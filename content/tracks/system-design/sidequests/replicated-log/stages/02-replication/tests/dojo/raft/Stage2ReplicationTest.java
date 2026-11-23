package dojo.raft;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class Stage2ReplicationTest {
    private static final List<String> THREE = List.of("n1", "n2", "n3");

    private static List<String> commands(RaftNode node) {
        return node.log().stream().map(Entry::command).toList();
    }

    @Test
    void theLeaderAppendsAndReturnsTheIndex() {
        var cluster = new Cluster(3);
        RaftNode leader = cluster.node(cluster.awaitLeader(20));
        assertEquals(1, leader.propose("a"));
        assertEquals(2, leader.propose("b"));
        assertEquals(List.of(new Entry(1, "a"), new Entry(1, "b")), leader.log());
        assertEquals(2, leader.lastIndex());
        assertEquals(0, leader.commitIndex(), "nothing is committed before a majority has it");
    }

    @Test
    void entriesReachTheFollowersAndAreCommitted() {
        var cluster = new Cluster(3);
        RaftNode leader = cluster.node(cluster.awaitLeader(20));
        leader.propose("a");
        leader.propose("b");
        leader.propose("c");
        cluster.deliver();
        assertEquals(List.of("a", "b", "c"), commands(cluster.node("n2")));
        assertEquals(3, leader.commitIndex(), "a majority answered");
        cluster.tick(Cluster.HEARTBEAT);
        cluster.assertSameLogs();
        assertEquals(3, cluster.node("n3").commitIndex(), "followers learn the commit from the next message");
    }

    @Test
    void aFollowerRefusesToProposeAndNamesTheLeader() {
        var cluster = new Cluster(3);
        cluster.awaitLeader(20);
        var refused = assertThrows(NotLeaderException.class, () -> cluster.node("n2").propose("x"));
        assertEquals("n1", refused.leaderHint());
        RaftNode lonely = new RaftNode("n2", THREE, new RaftConfig(10, 3, 0), new MemoryStorage());
        assertNull(assertThrows(NotLeaderException.class, () -> lonely.propose("x")).leaderHint());
    }

    @Test
    void aMajorityIsEnoughAndALateFollowerCatchesUp() {
        var cluster = new Cluster(3);
        RaftNode leader = cluster.node(cluster.awaitLeader(20));
        cluster.isolate("n3");
        for (int i = 0; i < 20; i++) {
            leader.propose("cmd-" + i);
        }
        cluster.tick(Cluster.HEARTBEAT);
        assertEquals(20, leader.commitIndex());
        assertEquals(20, cluster.node("n2").commitIndex());
        assertEquals(0, cluster.node("n3").lastIndex());
        cluster.heal();
        cluster.tick(2 * Cluster.HEARTBEAT);
        cluster.assertSameLogs();
    }

    @Test
    void aLeaderInTheMinorityCommitsNothingAndItsEntriesAreReplaced() {
        var cluster = new Cluster(5);
        RaftNode old = cluster.node(cluster.awaitLeader(20));
        old.propose("before");
        cluster.tick(Cluster.HEARTBEAT);
        assertEquals(1, old.commitIndex());
        cluster.partition(Set.of("n1", "n2"), Set.of("n3", "n4", "n5"));
        old.propose("lost-1");
        old.propose("lost-2");
        cluster.tick(60);
        assertEquals(1, old.commitIndex(), "2 of 5 is not a majority");
        assertEquals(3, cluster.node("n2").lastIndex(), "n2 has the uncommitted entries too");
        RaftNode fresh = cluster.leader();
        assertEquals("n3", cluster.leaderId());
        fresh.propose("kept");
        cluster.tick(Cluster.HEARTBEAT);
        assertEquals(2, fresh.commitIndex());
        cluster.heal();
        cluster.awaitLeader(60);
        cluster.tick(3 * Cluster.HEARTBEAT);
        cluster.assertSameLogs();
        assertEquals(List.of("before", "kept"), commands(cluster.node("n1")),
                "the conflicting entries of the old leader are gone");
    }

    @Test
    void conflictingSuffixesAreTruncatedOnlyWhereTheyConflict() {
        RaftNode n2 = new RaftNode("n2", THREE, new RaftConfig(10, 3, 0), new MemoryStorage());
        List<Entry> first = List.of(new Entry(1, "a"), new Entry(1, "b"), new Entry(1, "c"));
        n2.receive(new Envelope("n1", "n2", new Msg.AppendEntries(1, "n1", 0, 0, first, 0)));
        assertEquals(List.of(new Envelope("n2", "n1", new Msg.AppendReply(1, "n2", true, 3))), n2.drainOutbox());
        // a late copy of an older message must not cut the log
        n2.receive(new Envelope("n1", "n2", new Msg.AppendEntries(1, "n1", 0, 0, first.subList(0, 1), 0)));
        assertEquals(3, n2.lastIndex());
        assertEquals(List.of(new Envelope("n2", "n1", new Msg.AppendReply(1, "n2", true, 1))), n2.drainOutbox());
        // a new leader of term 2 has another entry at index 3
        n2.receive(new Envelope("n3", "n2",
                new Msg.AppendEntries(2, "n3", 2, 1, List.of(new Entry(2, "x")), 2)));
        assertEquals(List.of(new Entry(1, "a"), new Entry(1, "b"), new Entry(2, "x")), n2.log());
        assertEquals(2, n2.commitIndex());
        n2.drainOutbox();
        // a gap: prevLogIndex past its log
        n2.receive(new Envelope("n3", "n2", new Msg.AppendEntries(2, "n3", 7, 2, List.of(), 2)));
        assertEquals(List.of(new Envelope("n2", "n3", new Msg.AppendReply(2, "n2", false, 3))), n2.drainOutbox());
        // a mismatch: the term at prevLogIndex differs
        n2.receive(new Envelope("n3", "n2", new Msg.AppendEntries(2, "n3", 3, 1, List.of(), 2)));
        assertEquals(List.of(new Envelope("n2", "n3", new Msg.AppendReply(2, "n2", false, 2))), n2.drainOutbox());
    }

    @Test
    void theCommitIndexOfAFollowerNeverPassesWhatItHas() {
        RaftNode n2 = new RaftNode("n2", THREE, new RaftConfig(10, 3, 0), new MemoryStorage());
        n2.receive(new Envelope("n1", "n2",
                new Msg.AppendEntries(1, "n1", 0, 0, List.of(new Entry(1, "a")), 9)));
        assertEquals(1, n2.commitIndex());
    }

    @Test
    void entriesOfAnOlderTermAreCommittedOnlyWithOneOfTheCurrentTerm() {
        RaftNode n1 = new RaftNode("n1", THREE, new RaftConfig(10, 3, 0), new MemoryStorage());
        // n1 got an entry of term 1 from an old leader, then wins term 2
        n1.receive(new Envelope("n3", "n1",
                new Msg.AppendEntries(1, "n3", 0, 0, List.of(new Entry(1, "old")), 0)));
        for (int i = 0; i < 10; i++) {
            n1.tick();
        }
        n1.receive(new Envelope("n2", "n1", new Msg.Vote(2, "n2", true)));
        assertEquals(Role.LEADER, n1.role());
        n1.drainOutbox();
        n1.receive(new Envelope("n2", "n1", new Msg.AppendReply(2, "n2", true, 1)));
        assertEquals(0, n1.commitIndex(), "a majority has index 1, but its term is 1, not 2");
        n1.propose("new");
        n1.receive(new Envelope("n2", "n1", new Msg.AppendReply(2, "n2", true, 2)));
        assertEquals(2, n1.commitIndex(), "committing the term-2 entry commits the older one too");
    }

    @Test
    void manyProposalsUnderLostMessagesStillConverge() {
        var cluster = new Cluster(5);
        RaftNode leader = cluster.node(cluster.awaitLeader(20));
        int[] count = {0};
        cluster.drop = e -> e.msg() instanceof Msg.AppendEntries && ++count[0] % 3 == 0;
        List<String> sent = new ArrayList<>();
        for (int i = 0; i < 60; i++) {
            leader.propose("cmd-" + i);
            sent.add("cmd-" + i);
            cluster.tick(1);
        }
        cluster.drop = e -> false;
        cluster.tick(4 * Cluster.HEARTBEAT);
        assertEquals("n1", cluster.leaderId());
        cluster.assertSameLogs();
        assertEquals(sent, cluster.committedCommands());
        assertTrue(leader.commitIndex() == 60);
    }
}
