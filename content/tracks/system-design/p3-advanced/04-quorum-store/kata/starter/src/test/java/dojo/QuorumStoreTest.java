package dojo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class QuorumStoreTest {
    @Test
    void a_value_written_with_a_quorum_can_be_read() {
        var replicas = List.of(new Replica("r0"), new Replica("r1"), new Replica("r2"));
        var store = new QuorumStore(replicas, 2, 2);
        assertTrue(store.overlapping());
        store.put("cart:1", "book");
        assertEquals(Optional.of("book"), store.get("cart:1"));
    }
}
