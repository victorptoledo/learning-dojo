package dojo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class QuorumStoreGradingTest {
    private final Replica r0 = new Replica("r0");
    private final Replica r1 = new Replica("r1");
    private final Replica r2 = new Replica("r2");
    private final List<Replica> replicas = List.of(r0, r1, r2);

    @Test
    void overlap_is_w_plus_r_greater_than_n() {
        assertTrue(new QuorumStore(replicas, 2, 2).overlapping());
        assertTrue(new QuorumStore(replicas, 3, 1).overlapping());
        assertFalse(new QuorumStore(replicas, 1, 1).overlapping());
        assertFalse(new QuorumStore(replicas, 1, 2).overlapping());
    }

    @Test
    void versions_grow_and_writes_reach_every_replica_that_is_up() {
        var store = new QuorumStore(replicas, 2, 2);
        assertEquals(1, store.put("k", "a"));
        assertEquals(2, store.put("k", "b"));
        for (Replica replica : replicas) {
            assertEquals(Optional.of(new Versioned("b", 2)), replica.stored("k"));
        }
    }

    @Test
    void a_write_succeeds_with_w_acknowledgements_and_fails_below() {
        var store = new QuorumStore(replicas, 2, 2);
        r0.down();
        assertEquals(1, store.put("k", "a"), "two of three acknowledge");
        r1.down();
        assertThrows(QuorumNotReachedException.class, () -> store.put("k", "b"));
        assertEquals(Optional.of(new Versioned("b", 2)), r2.stored("k"), "a failed write is not rolled back");
        assertEquals(Optional.empty(), r0.stored("k"));
        r0.up();
        r1.up();
        assertEquals(3, store.put("k", "c"), "failed writes still use up a version");
    }

    @Test
    void overlapping_quorums_hide_a_failed_replica_and_repair_it() {
        var store = new QuorumStore(replicas, 2, 2);
        r0.down();
        store.put("cart", "2 books");
        r0.up();
        r1.down();
        assertEquals(Optional.of("2 books"), store.get("cart"), "r2 is in both quorums");
        assertEquals(Optional.of(new Versioned("2 books", 1)), r0.stored("cart"), "r0 was repaired");
        assertEquals(1, store.readRepairs());
    }

    @Test
    void without_overlap_a_read_can_be_stale() {
        var store = new QuorumStore(replicas, 1, 1);
        store.put("price", "10");
        r1.down();
        r2.down();
        store.put("price", "12");
        r0.down();
        r1.up();
        r2.up();
        assertEquals(Optional.of("10"), store.get("price"), "W + R = 2 is not more than N = 3");
    }

    @Test
    void a_read_asks_only_r_replicas_in_order() {
        var store = new QuorumStore(replicas, 3, 2);
        store.put("k", "v");
        store.get("k");
        assertEquals(1, r0.reads());
        assertEquals(1, r1.reads());
        assertEquals(0, r2.reads(), "two answers are enough");
        r0.down();
        store.get("k");
        assertEquals(2, r1.reads());
        assertEquals(1, r2.reads(), "the next replica replaces the one that is down");
    }

    @Test
    void read_repair_fixes_only_stale_or_missing_answers() {
        var store = new QuorumStore(replicas, 1, 3);
        r1.down();
        r2.down();
        store.put("k", "new");
        r1.up();
        r2.up();
        int writesOnR0 = r0.writes();
        assertEquals(Optional.of("new"), store.get("k"));
        assertEquals(2, store.readRepairs());
        assertEquals(writesOnR0, r0.writes(), "r0 was already up to date");
        assertEquals(Optional.of(new Versioned("new", 1)), r2.stored("k"));
        store.get("k");
        assertEquals(2, store.readRepairs(), "nothing left to repair");
    }

    @Test
    void a_missing_key_reads_as_empty_and_needs_no_repair() {
        var store = new QuorumStore(replicas, 2, 2);
        assertEquals(Optional.empty(), store.get("nothing"));
        assertEquals(0, store.readRepairs());
    }

    @Test
    void a_read_fails_without_r_answers() {
        var store = new QuorumStore(replicas, 1, 2);
        store.put("k", "v");
        r0.down();
        r1.down();
        assertThrows(QuorumNotReachedException.class, () -> store.get("k"));
    }

    @Test
    void impossible_quorums_are_refused() {
        assertThrows(IllegalArgumentException.class, () -> new QuorumStore(List.of(), 1, 1));
        assertThrows(IllegalArgumentException.class, () -> new QuorumStore(replicas, 0, 1));
        assertThrows(IllegalArgumentException.class, () -> new QuorumStore(replicas, 4, 1));
        assertThrows(IllegalArgumentException.class, () -> new QuorumStore(replicas, 1, 0));
        assertThrows(IllegalArgumentException.class, () -> new QuorumStore(replicas, 1, 4));
    }
}
