package dojo.broker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;

class Stage2GroupsTest {
    private static final Duration SESSION = Duration.ofSeconds(10);

    private final ManualClock clock = new ManualClock();
    private final Broker broker = new Broker(clock, new BrokerConfig(SESSION, 3));

    private void topic(int partitions, int messagesPerPartition) {
        broker.createTopic("orders", partitions, Duration.ofDays(1));
        for (int i = 0; i < partitions * messagesPerPartition; i++) {
            broker.send("orders", null, "m" + i);
        }
    }

    private static Map<Integer, String> assignment(Object... pairs) {
        Map<Integer, String> map = new TreeMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            map.put((Integer) pairs[i], (String) pairs[i + 1]);
        }
        return map;
    }

    private static List<String> values(Batch batch) {
        return batch.messages().stream().map(Message::value).toList();
    }

    /** Commits everything the batch returned, partition by partition. */
    private void commitAll(String member, Batch batch) {
        Map<Integer, Long> next = new HashMap<>();
        for (Message m : batch.messages()) {
            next.merge(m.partition(), m.offset() + 1, Math::max);
        }
        next.forEach((p, offset) -> broker.commit("billing", member, batch.generation(), p, offset));
    }

    @Test
    void partitionsAreAssignedByRanges() {
        topic(5, 0);
        broker.join("billing", "orders", "b");
        assertEquals(1, broker.generation("billing"));
        assertEquals(assignment(0, "b", 1, "b", 2, "b", 3, "b", 4, "b"), broker.assignment("billing"));
        broker.join("billing", "orders", "a");
        assertEquals(2, broker.generation("billing"));
        assertEquals(assignment(0, "a", 1, "a", 2, "a", 3, "b", 4, "b"), broker.assignment("billing"));
        broker.join("billing", "orders", "c");
        assertEquals(assignment(0, "a", 1, "a", 2, "b", 3, "b", 4, "c"), broker.assignment("billing"));
        broker.join("billing", "orders", "a");
        assertEquals(3, broker.generation("billing"), "joining again changes nothing");
        broker.leave("billing", "b");
        assertEquals(4, broker.generation("billing"));
        assertEquals(assignment(0, "a", 1, "a", 2, "a", 3, "c", 4, "c"), broker.assignment("billing"));
    }

    @Test
    void moreMembersThanPartitionsLeavesSomeIdle() {
        topic(2, 3);
        for (String m : List.of("d", "c", "b", "a")) {
            broker.join("billing", "orders", m);
        }
        assertEquals(assignment(0, "a", 1, "b"), broker.assignment("billing"));
        assertEquals(List.of(), broker.poll("billing", "d", 10).messages());
    }

    @Test
    void pollReadsTheMembersPartitionsLowestFirst() {
        topic(4, 3); // partition p gets m(p), m(p+4), m(p+8)
        broker.join("billing", "orders", "a");
        broker.join("billing", "orders", "b"); // a: 0, 1 · b: 2, 3
        Batch batch = broker.poll("billing", "a", 4);
        assertEquals(2, batch.generation());
        assertEquals(List.of("m0", "m4", "m8", "m1"), values(batch));
        assertEquals(List.of("m5", "m9"), values(broker.poll("billing", "a", 4)));
        assertEquals(List.of(), values(broker.poll("billing", "a", 4)));
        assertEquals(List.of("m2", "m6", "m10", "m3", "m7", "m11"), values(broker.poll("billing", "b", 10)));
    }

    @Test
    void aNewOwnerStartsFromTheCommittedOffset() {
        topic(1, 10);
        broker.join("billing", "orders", "a");
        Batch first = broker.poll("billing", "a", 4);
        commitAll("a", first);
        assertEquals(4, broker.committed("billing", 0));
        broker.poll("billing", "a", 3); // m4..m6 read but never committed
        broker.leave("billing", "a");
        broker.join("billing", "orders", "b");
        assertEquals(List.of("m4", "m5", "m6", "m7", "m8", "m9"), values(broker.poll("billing", "b", 100)),
                "at-least-once: what a was doing is read again");
    }

    @Test
    void aMemberThatStopsPollingIsRemovedAfterTheSessionTimeout() {
        topic(2, 5);
        broker.join("billing", "orders", "a");
        broker.join("billing", "orders", "b");
        broker.poll("billing", "a", 1);
        broker.poll("billing", "b", 1);
        clock.advance(SESSION.minusSeconds(1));
        broker.poll("billing", "a", 1);
        assertEquals(assignment(0, "a", 1, "b"), broker.assignment("billing"), "b still has a second");
        clock.advance(Duration.ofSeconds(1));
        assertEquals(assignment(0, "a", 1, "a"), broker.assignment("billing"), "b crashed: a takes over");
        assertEquals(3, broker.generation("billing"));
        List<String> rest = values(broker.poll("billing", "a", 100));
        assertTrue(rest.contains("m1"), "b never committed: its first message comes again " + rest);
        assertThrows(IllegalStateException.class, () -> broker.poll("billing", "b", 1), "a dead member must join again");
    }

    @Test
    void aZombieCannotCommitAfterARebalance() {
        topic(2, 5);
        broker.join("billing", "orders", "a");
        Batch stale = broker.poll("billing", "a", 10);
        broker.join("billing", "orders", "b"); // a loses partition 1
        assertThrows(CommitFailedException.class, () -> broker.commit("billing", "a", stale.generation(), 1, 5));
        assertThrows(CommitFailedException.class, () -> broker.commit("billing", "a", stale.generation(), 0, 5),
                "an old generation is refused even for a partition it still has");
        assertEquals(-1, broker.committed("billing", 1));
        assertEquals(-1, broker.committed("billing", 0));
        int now = broker.generation("billing");
        assertThrows(CommitFailedException.class, () -> broker.commit("billing", "a", now, 1, 5), "not a's partition");
        broker.commit("billing", "a", now, 0, 5);
        assertEquals(5, broker.committed("billing", 0));
        assertThrows(IllegalArgumentException.class, () -> broker.commit("billing", "a", now, 0, 6));
        assertThrows(IllegalArgumentException.class, () -> broker.commit("billing", "a", now, 0, -1));
    }

    @Test
    void positionsGoBackToTheCommitAfterEveryRebalance() {
        topic(2, 5);
        broker.join("billing", "orders", "a");
        Batch batch = broker.poll("billing", "a", 3); // m0 m2 m4 from partition 0
        broker.commit("billing", "a", batch.generation(), 0, 2);
        broker.join("billing", "orders", "b");
        assertEquals(List.of("m4", "m6", "m8"), values(broker.poll("billing", "a", 10)),
                "a keeps partition 0 but goes back to its commit");
    }

    @Test
    void aCommitOlderThanRetentionStartsAtTheOldestKeptMessage() {
        broker.createTopic("orders", 1, Duration.ofHours(1));
        for (int i = 0; i < 3; i++) {
            broker.send("orders", "k", "old" + i);
        }
        broker.join("billing", "orders", "a");
        broker.commit("billing", "a", broker.generation("billing"), 0, 1);
        broker.leave("billing", "a");
        clock.advance(Duration.ofMinutes(30));
        broker.send("orders", "k", "new");
        clock.advance(Duration.ofMinutes(30));
        broker.enforceRetention();
        broker.join("billing", "orders", "a");
        assertEquals(List.of("new"), values(broker.poll("billing", "a", 10)));
    }

    @Test
    void groupsAreIndependent() {
        topic(1, 4);
        broker.join("billing", "orders", "a");
        broker.join("audit", "orders", "a");
        commitAll("a", broker.poll("billing", "a", 4));
        assertEquals(4, broker.poll("audit", "a", 10).messages().size());
        assertEquals(-1, broker.committed("audit", 0));
        broker.createTopic("payments", 1, Duration.ofDays(1));
        assertThrows(IllegalArgumentException.class, () -> broker.join("billing", "payments", "b"));
    }
}
