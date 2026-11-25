package dojo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** A first look at the store with one partition: a leader and two followers. */
class ReplicatedStoreTest {
    private final Node leader = new Node("a1");
    private final Node f1 = new Node("a2");
    private final Node f2 = new Node("a3");
    private final ReplicatedStore store = new ReplicatedStore(List.of(List.of(leader, f1, f2)));

    @Test
    void you_read_your_own_write_even_before_it_is_replicated() {
        Session ada = store.session();
        assertEquals(1, store.put(ada, "profile:ada", "v1"));
        assertEquals(Optional.of("v1"), store.get(ada, "profile:ada"), "the followers are behind: ask the leader");
        assertEquals(Optional.empty(), store.get(store.session(), "profile:ada"), "a new session may read a stale follower");
        assertEquals(1, store.lag("a2"));

        assertEquals(2, store.replicate(10), "one entry to each follower");
        assertEquals(0, store.lag("a2"));
        int leaderReads = leader.reads();
        assertEquals(Optional.of("v1"), store.get(ada, "profile:ada"));
        assertEquals(leaderReads, leader.reads(), "a caught-up follower answers now");
    }

    @Test
    void failover_promotes_the_most_advanced_follower_and_reports_what_was_lost() {
        Session s = store.session();
        store.put(s, "k", "1");
        store.put(s, "k", "2");
        store.replicate(1);                 // a2 and a3 at 1
        f1.crash();
        store.replicate(1);                 // a3 at 2
        store.put(s, "k", "3");             // only the leader has 3
        f1.restart();
        leader.crash();

        Failover failover = store.failover(0);
        assertEquals(new Failover(0, "a1", "a3", 2, 1), failover);
        assertEquals("a3", store.leaderOf(0));
        assertThrows(LostWriteException.class, () -> store.get(s, "k"), "this session saw write 3, which is gone");
        store.replicate(10);                // a2 catches up from the new leader
        assertEquals(Optional.of("2"), store.get(store.session(), "k"));
    }
}
