package dojo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ReplicatedStoreGradingTest {

    /** The n-th key (0, 1, 2, ...) that lives in a partition. */
    private static String keyIn(ReplicatedStore store, int partition, int n) {
        int found = 0;
        for (int i = 0; ; i++) {
            String key = "key-" + i;
            if (store.partitionFor(key) == partition && found++ == n) {
                return key;
            }
        }
    }

    private static List<Node> nodes(String... names) {
        List<Node> nodes = new ArrayList<>();
        for (String name : names) {
            nodes.add(new Node(name));
        }
        return nodes;
    }

    // ---- construction and routing ---------------------------------------------------

    @Test
    void the_constructor_rejects_empty_and_repeated_nodes() {
        assertThrows(IllegalArgumentException.class, () -> new ReplicatedStore(List.of()));
        assertThrows(IllegalArgumentException.class, () -> new ReplicatedStore(List.of(nodes("a"), List.of())));
        assertThrows(IllegalArgumentException.class, () -> new ReplicatedStore(List.of(nodes("a", "b"), nodes("c", "a"))),
                "a node serves one partition only");
    }

    @Test
    void keys_go_to_the_leader_of_their_partition() {
        List<List<Node>> partitions = List.of(nodes("a1", "a2"), nodes("b1", "b2"), nodes("c1", "c2"));
        var store = new ReplicatedStore(partitions);
        Session session = store.session();
        long[] expected = new long[3];
        for (int i = 0; i < 60; i++) {
            String key = "user:" + i;
            int partition = store.partitionFor(key);
            assertEquals(Math.floorMod(Hashing.hash64(key), 3), partition, key);
            assertEquals(++expected[partition], store.put(session, key, "v" + i), "positions count per partition");
        }
        for (int p = 0; p < 3; p++) {
            assertEquals(expected[p], partitions.get(p).get(0).position(), "only the leader of " + p + " was written");
            assertEquals(0, partitions.get(p).get(1).position(), "followers wait for replicate()");
            assertTrue(expected[p] > 0);
        }
        assertEquals("b1", store.leaderOf(1));
        assertEquals(1, store.epochOf(2));
        assertThrows(IllegalArgumentException.class, () -> store.leaderOf(3));
    }

    // ---- replication ----------------------------------------------------------------

    @Test
    void replicate_ships_entries_in_order_up_to_the_limit() {
        List<Node> group = nodes("a", "b", "c");
        var store = new ReplicatedStore(List.of(group));
        Session s = store.session();
        for (int i = 1; i <= 5; i++) {
            store.put(s, "k" + i, "v" + i);
        }
        assertEquals(5, store.lag("b"));
        assertEquals(0, store.lag("a"), "the leader is never behind itself");
        assertThrows(IllegalArgumentException.class, () -> store.replicate(0));
        assertThrows(IllegalArgumentException.class, () -> store.lag("zed"));

        assertEquals(4, store.replicate(2));
        assertEquals(3, store.lag("b"));
        assertEquals(3, store.lag("c"));
        group.get(2).crash();
        assertEquals(3, store.replicate(10), "a follower that is down is skipped");
        assertEquals(0, store.lag("b"));
        group.get(2).restart();
        assertEquals(3, store.replicate(10));
        assertEquals(0, store.replicate(10));
        assertEquals(Optional.of("v5"), group.get(2).get("k5"));
    }

    // ---- session guarantees ---------------------------------------------------------

    @Test
    void read_your_writes_picks_the_first_follower_that_has_the_write() {
        List<Node> group = nodes("a", "b", "c");
        Node leader = group.get(0), b = group.get(1), c = group.get(2);
        var store = new ReplicatedStore(List.of(group));
        Session ada = store.session();
        store.put(ada, "k", "old");
        store.replicate(10);
        store.put(ada, "k", "new");
        b.crash();
        store.replicate(10);           // only c has "new"
        b.restart();

        assertEquals(Optional.of("new"), store.get(ada, "k"));
        assertEquals(0, b.reads(), "b is behind the session");
        assertEquals(1, c.reads());
        assertEquals(0, leader.reads(), "no need to load the leader when a follower has the write");

        assertEquals(Optional.of("old"), store.get(store.session(), "k"), "a fresh session takes the first follower, b");
        assertEquals(1, b.reads());

        store.put(ada, "k", "newest");
        assertEquals(Optional.of("newest"), store.get(ada, "k"), "nobody has it yet: the leader answers");
        assertEquals(1, leader.reads());
    }

    @Test
    void monotonic_reads_never_go_back_in_time() {
        List<Node> group = nodes("a", "b", "c");
        Node b = group.get(1), c = group.get(2);
        var store = new ReplicatedStore(List.of(group));
        Session writer = store.session();
        store.put(writer, "comment", "first");
        store.replicate(10);
        store.put(writer, "comment", "edited");
        b.crash();
        store.replicate(10);           // b at 1, c at 2
        b.restart();
        c.crash();

        Session reader = store.session();
        assertEquals(Optional.of("first"), store.get(reader, "comment"), "b is the only follower up");
        c.restart();
        assertEquals(Optional.of("first"), store.get(reader, "comment"));

        b.crash();
        assertEquals(Optional.of("edited"), store.get(reader, "comment"), "c is ahead: the reader moves forward");
        b.restart();
        int bReads = b.reads();
        assertEquals(Optional.of("edited"), store.get(reader, "comment"), "b is behind what the reader saw");
        assertEquals(bReads, b.reads(), "the reader must not be sent back to b");
        Optional<Token> token = reader.token(0);
        assertEquals(Optional.of(new Token(1, 2)), token);
    }

    @Test
    void followers_keep_serving_reads_while_the_leader_is_down() {
        List<Node> group = nodes("a", "b");
        var store = new ReplicatedStore(List.of(group));
        Session old = store.session();
        store.put(old, "k", "1");
        store.replicate(10);
        Session fresh = store.session();
        store.put(fresh, "k", "2");
        group.get(0).crash();

        assertEquals(Optional.of("1"), store.get(old, "k"), "b has everything this session saw");
        assertEquals(Optional.of("1"), store.get(store.session(), "k"));
        assertThrows(LeaderUnavailableException.class, () -> store.get(fresh, "k"), "only the leader had write 2");
        assertThrows(LeaderUnavailableException.class, () -> store.put(old, "k", "3"));
        assertEquals(0, store.replicate(10), "a partition without a leader replicates nothing");
    }

    // ---- failover -------------------------------------------------------------------

    @Test
    void failover_needs_a_dead_leader_and_a_live_follower() {
        List<Node> group = nodes("a", "b", "c");
        var store = new ReplicatedStore(List.of(group));
        assertThrows(IllegalStateException.class, () -> store.failover(0), "the leader is up");
        assertThrows(IllegalArgumentException.class, () -> store.failover(1));
        group.get(0).crash();
        group.get(1).crash();
        group.get(2).crash();
        assertThrows(LeaderUnavailableException.class, () -> store.failover(0));
        assertEquals("a", store.leaderOf(0), "a failed failover changes nothing");
        assertEquals(1, store.epochOf(0));
    }

    @Test
    void failover_takes_the_most_advanced_follower_ties_by_list_order() {
        List<Node> group = nodes("a", "b", "c", "d");
        var store = new ReplicatedStore(List.of(group));
        Session s = store.session();
        store.put(s, "k", "1");
        store.put(s, "k", "2");
        store.replicate(10);
        group.get(0).crash();

        assertEquals(new Failover(0, "a", "b", 2, 0), store.failover(0), "b, c and d tie at 2: b comes first");
        assertEquals(Optional.of("2"), store.get(s, "k"));
        assertEquals(3, store.put(s, "k", "3"), "the new leader continues the log");
        assertEquals(Optional.of(new Token(2, 3)), s.token(0));
        assertEquals(LogEntry.class, group.get(1).entriesAfter(2, 1).get(0).getClass());
        assertEquals(2, group.get(1).entriesAfter(2, 1).get(0).epoch(), "new writes carry the new epoch");
        assertEquals(2, store.replicate(10), "c and d follow b; a is down");
    }

    @Test
    void a_session_whose_writes_survived_keeps_its_guarantees() {
        List<Node> group = nodes("a", "b", "c");
        var store = new ReplicatedStore(List.of(group));
        Session kept = store.session();
        store.put(kept, "k", "kept");
        store.replicate(10);
        Session lost = store.session();
        store.put(lost, "k", "lost");
        group.get(0).crash();

        Failover failover = store.failover(0);
        assertEquals(1, failover.lostWrites(), "the leader had acknowledged one write nobody else had");
        assertEquals(Optional.of("kept"), store.get(kept, "k"));
        assertEquals(2, store.put(kept, "k", "after"));
        assertEquals(Optional.of("after"), store.get(kept, "k"));

        assertThrows(LostWriteException.class, () -> store.get(lost, "k"));
        assertThrows(LostWriteException.class, () -> store.put(lost, "k", "again"), "every call fails: start a new session");
        assertThrows(LostWriteException.class, () -> store.get(lost, "k"));
    }

    @Test
    void partitions_fail_over_independently() {
        List<List<Node>> partitions = List.of(nodes("a1", "a2"), nodes("b1", "b2"));
        var store = new ReplicatedStore(partitions);
        Session s = store.session();
        String inA = keyIn(store, 0, 0);
        String inB = keyIn(store, 1, 0);
        store.put(s, inA, "a");
        store.put(s, inB, "b");      // neither is replicated
        partitions.get(0).get(0).crash();
        store.failover(0);

        assertThrows(LostWriteException.class, () -> store.get(s, inA));
        assertEquals(Optional.of("b"), store.get(s, inB), "partition 1 did not fail over");
        assertEquals(1, store.epochOf(1));
        assertEquals(2, store.epochOf(0));
    }

    @Test
    void an_old_leader_comes_back_without_its_lost_writes() {
        List<Node> group = nodes("a", "b");
        Node a = group.get(0), b = group.get(1);
        var store = new ReplicatedStore(List.of(group));
        Session s = store.session();
        store.put(s, "k", "1");
        store.replicate(10);
        store.put(s, "k", "lost-2");
        store.put(s, "k", "lost-3");
        store.put(s, "other", "lost-4");
        a.crash();
        assertEquals(3, store.failover(0).lostWrites());
        a.restart();

        Session fresh = store.session();
        assertEquals(Optional.of("1"), store.get(fresh, "k"));
        assertEquals(0, a.reads(), "a still holds lost writes: it must not answer");
        assertEquals("b", store.leaderOf(0));

        store.put(fresh, "k", "2");
        assertEquals(1, store.replicate(10), "a is cut back to position 1 and then gets the new write");
        assertEquals(3, store.truncatedEntries());
        assertEquals(2, a.position());
        assertEquals(Optional.of("2"), a.get("k"));
        assertEquals(Optional.empty(), a.get("other"), "the lost key is gone too");
        assertEquals(0, store.lag("a"));
    }

    @Test
    void only_writes_that_every_later_leader_had_survive_a_chain_of_failovers() {
        List<Node> group = nodes("a", "b", "c", "d");
        Node a = group.get(0), b = group.get(1), c = group.get(2), d = group.get(3);
        var store = new ReplicatedStore(List.of(group));
        Session s3 = null;
        Session s4 = null;
        for (int i = 1; i <= 5; i++) {
            Session s = store.session();
            store.put(s, "k", "v" + i);
            if (i == 3) s3 = s;
            if (i == 4) s4 = s;
        }
        store.replicate(3);            // b, c, d at 3
        c.crash();
        d.crash();
        store.replicate(10);           // b at 5
        a.crash();
        assertEquals(new Failover(0, "a", "b", 2, 0), store.failover(0));
        Session s6 = store.session();
        assertEquals(6, store.put(s6, "k", "v6"));

        b.crash();
        c.restart();
        assertEquals(new Failover(0, "b", "c", 3, 3), store.failover(0), "c only had 3 of b's 6 entries");

        Session s3Final = s3;
        Session s4Final = s4;
        assertEquals(Optional.of("v3"), store.get(s3Final, "k"), "write 3 is in every later leader's log");
        assertThrows(LostWriteException.class, () -> store.get(s4Final, "k"), "write 4 survived epoch 2 but not epoch 3");
        assertThrows(LostWriteException.class, () -> store.get(s6, "k"));

        a.restart();
        b.restart();
        d.restart();
        assertEquals(4, store.put(store.session(), "k", "v7"));
        store.replicate(10);
        assertEquals(2 + 3, store.truncatedEntries(), "a drops 4 and 5, b drops 4, 5 and 6; d had nothing to drop");
        for (Node node : List.of(a, b, d)) {
            assertEquals(4, node.position(), node.name());
            assertEquals(Optional.of("v7"), node.get("k"), node.name());
        }
        assertFalse(store.get(store.session(), "k").isEmpty());
    }

    // ---- structure ------------------------------------------------------------------

    @Test
    void the_store_keeps_no_global_state() {
        for (Field field : ReplicatedStore.class.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers()) && !field.isSynthetic()) {
                assertTrue(Modifier.isFinal(field.getModifiers()),
                        "static field " + field.getName() + " would be shared by every store");
            }
        }
    }
}
