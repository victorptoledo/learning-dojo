package dojo.raft;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class Stage4KeyValueTest {

    private final Cluster cluster = new Cluster(3);

    private RaftNode leader() {
        return cluster.node(cluster.awaitLeader(60));
    }

    private void settle() {
        cluster.tick(2 * Cluster.HEARTBEAT);
    }

    private void assertEverywhere(Map<String, String> expected) {
        for (String id : cluster.ids) {
            if (!cluster.crashed.contains(id)) {
                RaftNode n = cluster.node(id);
                assertEquals(expected, n.data(), id);
                assertEquals(n.commitIndex(), n.lastApplied(), id + " applies what it commits");
            }
        }
    }

    @Test
    void everyNodeAppliesTheCommittedCommandsInOrder() {
        RaftNode leader = leader();
        assertEquals(1, leader.submit("alice", 1, "set color blue"));
        leader.submit("alice", 2, "set city São Paulo");
        leader.submit("bob", 1, "set color green");
        leader.submit("bob", 2, "del city");
        leader.submit("bob", 3, "set motto keep it simple");
        assertEquals(Map.of(), leader.data(), "nothing is applied before it is committed");
        settle();
        assertEverywhere(Map.of("color", "green", "motto", "keep it simple"));
        assertEquals(5, cluster.node("n3").lastApplied());
    }

    @Test
    void otherCommandsAreAppliedAsNothing() {
        RaftNode leader = leader();
        leader.propose("hello");
        leader.propose("alice x set a 1");
        leader.submit("alice", 1, "rename a b");
        leader.submit("alice", 2, "set a 1");
        settle();
        assertEverywhere(Map.of("a", "1"));
        assertEquals(4, leader.lastApplied());
    }

    @Test
    void aRetriedCommandIsAppliedOnce() {
        RaftNode leader = leader();
        leader.submit("alice", 1, "set counter 1");
        leader.submit("alice", 1, "set counter 1");
        leader.submit("alice", 2, "set counter 2");
        leader.submit("alice", 1, "set counter 1"); // a very late retry
        leader.submit("bob", 1, "set other 1");
        settle();
        assertEverywhere(Map.of("counter", "2", "other", "1"));
    }

    @Test
    void aRetryThroughTheNextLeaderDoesNotUndoAnything() {
        RaftNode first = leader();
        first.submit("alice", 1, "set x 1");
        first.submit("alice", 2, "set x 2");
        settle();
        cluster.crash(cluster.leaderId());
        RaftNode next = leader();
        next.submit("alice", 2, "set x 2"); // alice never got the answer and retries
        next.submit("alice", 1, "set x 1");
        settle();
        assertEverywhere(Map.of("x", "2"));
    }

    @Test
    void readsComeFromALeaderThatCanProveIt() {
        RaftNode leader = leader();
        leader.submit("alice", 1, "set color blue");
        settle();
        assertEquals(Optional.of("blue"), leader.read("color"));
        assertEquals(Optional.empty(), leader.read("size"));
        var refused = assertThrows(NotLeaderException.class, () -> cluster.node("n2").read("color"));
        assertEquals("n1", refused.leaderHint(), "a follower points to the leader");
    }

    @Test
    void aLeaderCutOffFromTheMajorityStopsServingReads() {
        RaftNode old = leader();
        old.submit("alice", 1, "set color blue");
        settle();
        cluster.isolate("n1");
        cluster.tick(5);
        assertEquals(Optional.of("blue"), old.read("color"), "within its last electionTimeout it is still safe");
        cluster.tick(10);
        assertNull(assertThrows(NotLeaderException.class, () -> old.read("color")).leaderHint(),
                "no majority has answered for longer than an election timeout");
        cluster.tick(30);
        RaftNode fresh = cluster.leader();
        fresh.submit("bob", 1, "set color red");
        settle();
        assertEquals(Optional.of("red"), fresh.read("color"));
        assertThrows(NotLeaderException.class, () -> old.read("color"), "the stale leader never answers blue");
    }

    @Test
    void aNewLeaderWaitsForAnEntryOfItsTerm() {
        RaftNode first = leader();
        first.submit("alice", 1, "set color blue");
        settle();
        cluster.crash("n1");
        RaftNode next = leader();
        assertThrows(IllegalStateException.class, () -> next.read("color"),
                "it cannot know yet what the old leader committed");
        next.submit("alice", 2, "set size L");
        settle();
        assertEquals(Optional.of("blue"), next.read("color"));
    }
}
