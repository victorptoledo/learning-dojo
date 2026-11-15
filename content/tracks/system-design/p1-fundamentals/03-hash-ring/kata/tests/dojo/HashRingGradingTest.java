package dojo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;

class HashRingGradingTest {
    private static final int KEYS = 20_000;

    private static HashRing ring(int virtualNodes, String... nodes) {
        var ring = new HashRing(virtualNodes);
        for (String node : nodes) {
            ring.add(node);
        }
        return ring;
    }

    private static Map<String, String> owners(HashRing ring) {
        Map<String, String> owners = new HashMap<>();
        for (int i = 0; i < KEYS; i++) {
            owners.put("key-" + i, ring.nodeFor("key-" + i));
        }
        return owners;
    }

    @Test
    void invalid_rings_and_empty_rings_are_refused() {
        assertThrows(IllegalArgumentException.class, () -> new HashRing(0));
        var ring = new HashRing(10);
        assertThrows(IllegalStateException.class, () -> ring.nodeFor("k"));
        assertThrows(IllegalArgumentException.class, () -> ring.add(" "));
        assertThrows(IllegalArgumentException.class, () -> ring.add(null));
    }

    @Test
    void with_one_point_per_node_the_owner_is_the_next_point_clockwise() {
        var ring = ring(1, "a", "b", "c");
        var points = new TreeMap<Long, String>();
        for (String node : List.of("a", "b", "c")) {
            points.put(Hashing.hash64(node + "#0"), node);
        }
        for (int i = 0; i < 200; i++) {
            String key = "k" + i;
            var next = points.ceilingEntry(Hashing.hash64(key));
            String expected = (next != null ? next : points.firstEntry()).getValue();
            assertEquals(expected, ring.nodeFor(key), "owner of " + key);
        }
    }

    @Test
    void the_load_is_spread_evenly_with_virtual_nodes() {
        var counts = new HashMap<String, Integer>();
        owners(ring(200, "n1", "n2", "n3", "n4")).values().forEach(n -> counts.merge(n, 1, Integer::sum));
        assertEquals(4, counts.size());
        for (var entry : counts.entrySet()) {
            double share = entry.getValue() / (double) KEYS;
            assertTrue(share > 0.18 && share < 0.32, entry.getKey() + " owns " + share);
        }
    }

    @Test
    void adding_a_node_moves_few_keys_and_only_to_the_new_node() {
        var ring = ring(200, "n1", "n2", "n3");
        var before = owners(ring);
        ring.add("n4");
        var after = owners(ring);
        int moved = 0;
        for (var entry : before.entrySet()) {
            String now = after.get(entry.getKey());
            if (!now.equals(entry.getValue())) {
                moved++;
                assertEquals("n4", now, entry.getKey() + " moved between old nodes");
            }
        }
        double share = moved / (double) KEYS;
        assertTrue(share > 0.15 && share < 0.35, "moved " + share);
    }

    @Test
    void removing_a_node_moves_only_its_own_keys() {
        var ring = ring(200, "n1", "n2", "n3", "n4");
        var before = owners(ring);
        ring.remove("n2");
        var after = owners(ring);
        for (var entry : before.entrySet()) {
            if (entry.getValue().equals("n2")) {
                assertTrue(!after.get(entry.getKey()).equals("n2"));
            } else {
                assertEquals(entry.getValue(), after.get(entry.getKey()), entry.getKey() + " should not move");
            }
        }
        assertEquals(Set.of("n1", "n3", "n4"), ring.nodes());
    }

    @Test
    void adding_twice_and_removing_unknown_nodes_change_nothing() {
        var ring = ring(100, "n1", "n2");
        var before = owners(ring);
        ring.add("n1");
        ring.remove("ghost");
        assertEquals(before, owners(ring));
        assertEquals(List.of("n1", "n2"), List.copyOf(ring.nodes()));
        assertThrows(UnsupportedOperationException.class, () -> ring.nodes().add("x"));
        ring.remove("n1");
        ring.remove("n2");
        assertThrows(IllegalStateException.class, () -> ring.nodeFor("k"));
    }

    @Test
    void replicas_are_the_next_distinct_nodes_clockwise() {
        var ring = ring(50, "n1", "n2", "n3", "n4", "n5");
        for (int i = 0; i < 500; i++) {
            String key = "item-" + i;
            List<String> replicas = ring.nodesFor(key, 3);
            assertEquals(3, replicas.size());
            assertEquals(3, new HashSet<>(replicas).size(), "replicas must be distinct");
            assertEquals(ring.nodeFor(key), replicas.get(0), "the owner comes first");
        }
        assertEquals(5, ring.nodesFor("x", 9).size(), "never more than the nodes there are");
        assertEquals(List.of(), ring.nodesFor("x", 0));
        assertThrows(IllegalArgumentException.class, () -> ring.nodesFor("x", -1));
    }

    @Test
    void replicas_follow_the_ring_order() {
        var ring = ring(1, "a", "b", "c", "d");
        var points = new TreeMap<Long, String>();
        for (String node : List.of("a", "b", "c", "d")) {
            points.put(Hashing.hash64(node + "#0"), node);
        }
        long h = Hashing.hash64("some-key");
        var expected = new java.util.ArrayList<String>();
        expected.addAll(points.tailMap(h, true).values());
        expected.addAll(points.headMap(h, false).values());
        assertEquals(expected.subList(0, 3), ring.nodesFor("some-key", 3));
    }
}
