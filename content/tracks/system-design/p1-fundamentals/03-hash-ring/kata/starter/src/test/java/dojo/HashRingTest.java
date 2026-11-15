package dojo;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class HashRingTest {
    @Test
    void a_single_node_owns_every_key() {
        var ring = new HashRing(10);
        ring.add("cache-a");
        assertEquals("cache-a", ring.nodeFor("user:42"));
        assertEquals("cache-a", ring.nodeFor("user:43"));
    }

    @Test
    void the_same_key_always_lands_on_the_same_node() {
        var ring = new HashRing(50);
        ring.add("cache-a");
        ring.add("cache-b");
        ring.add("cache-c");
        assertEquals(ring.nodeFor("session:7"), ring.nodeFor("session:7"));
    }
}
