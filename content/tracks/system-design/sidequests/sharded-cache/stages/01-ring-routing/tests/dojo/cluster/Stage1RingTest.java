package dojo.cluster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dojo.cluster.Fakes.FakeNode;
import dojo.cluster.Fakes.FakeOrigin;
import dojo.cluster.Fakes.ManualClock;
import dojo.cluster.Fakes.TestRing;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class Stage1RingTest {
    private final ManualClock clock = new ManualClock();
    private final FakeOrigin origin = new FakeOrigin();
    private final CacheCluster cluster = new CacheCluster(origin, clock, Fakes.config());
    private final Map<String, FakeNode> nodes = new LinkedHashMap<>();

    private FakeNode add(String name, int weight) {
        var node = new FakeNode(name);
        nodes.put(name, node);
        cluster.addNode(name, node, weight);
        return node;
    }

    @Test
    void ownersFollowTheRingOfTheReadme() {
        add("cache-a", 1);
        add("cache-b", 1);
        add("cache-c", 2);
        var ring = new TestRing(100, Map.of("cache-a", 1, "cache-b", 1, "cache-c", 2));
        for (String key : Fakes.keys(2_000)) {
            assertEquals(ring.owner(key), cluster.ownerOf(key), key);
        }
    }

    @Test
    void weightsSetTheShareOfKeys() {
        add("small", 1);
        add("medium", 2);
        add("large", 3);
        Map<String, Integer> counts = new HashMap<>();
        for (String key : Fakes.keys(60_000)) {
            counts.merge(cluster.ownerOf(key), 1, Integer::sum);
        }
        assertTrue(Math.abs(counts.get("small") - 10_000) < 1_500, "small " + counts);
        assertTrue(Math.abs(counts.get("medium") - 20_000) < 2_500, "medium " + counts);
        assertTrue(Math.abs(counts.get("large") - 30_000) < 3_000, "large " + counts);
    }

    @Test
    void addingOrRemovingANodeMovesOnlyItsKeys() {
        add("a", 1);
        add("b", 1);
        add("c", 1);
        Map<String, String> before = new HashMap<>();
        for (String key : Fakes.keys(10_000)) {
            before.put(key, cluster.ownerOf(key));
        }
        cluster.addNode("d", new FakeNode("d"), 1);
        int moved = 0;
        for (String key : Fakes.keys(10_000)) {
            String now = cluster.ownerOf(key);
            if (!now.equals(before.get(key))) {
                assertEquals("d", now, "a key that moves goes to the new node");
                moved++;
            }
        }
        assertTrue(moved > 1_800 && moved < 3_200, "about a quarter moved: " + moved);
        cluster.removeNode("d");
        for (String key : Fakes.keys(10_000)) {
            assertEquals(before.get(key), cluster.ownerOf(key), "removing d gives its keys back");
        }
        cluster.removeNode("b");
        for (String key : Fakes.keys(10_000)) {
            if (!before.get(key).equals("b")) {
                assertEquals(before.get(key), cluster.ownerOf(key), "only b's keys move");
            }
        }
    }

    @Test
    void aMissLoadsFromTheDatabaseAndStoresOnTheOwner() {
        add("a", 1);
        add("b", 1);
        String owner = cluster.ownerOf("user:7");
        assertEquals("value-of-user:7", cluster.get("user:7"));
        assertEquals(1, origin.loads.get());
        assertEquals("value-of-user:7", nodes.get(owner).data.get("user:7"));
        String other = owner.equals("a") ? "b" : "a";
        assertTrue(nodes.get(other).data.isEmpty(), "only the owner stores it");
        assertEquals(0, nodes.get(other).calls.get(), "and only the owner is asked");
    }

    @Test
    void aHitNeverReachesTheDatabase() {
        add("a", 1);
        add("b", 1);
        add("c", 1);
        for (int round = 0; round < 5; round++) {
            for (String key : Fakes.keys(500)) {
                assertEquals("value-of-" + key, cluster.get(key));
            }
        }
        assertEquals(500, origin.loads.get());
        int stored = nodes.values().stream().mapToInt(n -> n.data.size()).sum();
        assertEquals(500, stored);
    }

    @Test
    void invalidateMakesTheNextReadLoadAgain() {
        add("a", 1);
        add("b", 1);
        cluster.get("user:1");
        origin.values.put("user:1", "changed");
        assertEquals("value-of-user:1", cluster.get("user:1"), "still cached");
        cluster.invalidate("user:1");
        assertFalse(nodes.get(cluster.ownerOf("user:1")).data.containsKey("user:1"));
        assertEquals("changed", cluster.get("user:1"));
        assertEquals(2, origin.loads.get());
        cluster.invalidate("never-read");
    }

    @Test
    void aRemovedNodeLeavesItsKeysColdOnTheNewOwners() {
        add("a", 1);
        add("b", 1);
        for (String key : Fakes.keys(200)) {
            cluster.get(key);
        }
        long onA = nodes.get("a").data.size();
        cluster.removeNode("a");
        nodes.get("a").resetCounts();
        for (String key : Fakes.keys(200)) {
            assertEquals("b", cluster.ownerOf(key));
            cluster.get(key);
        }
        assertEquals(200 + onA, origin.loads.get());
        assertEquals(0, nodes.get("a").calls.get(), "a removed node is never called");
    }

    @Test
    void badNodesAndEmptyClustersAreRefused() {
        assertThrows(IllegalStateException.class, () -> cluster.get("k"));
        assertThrows(IllegalStateException.class, () -> cluster.ownerOf("k"));
        assertThrows(IllegalArgumentException.class, () -> cluster.addNode("a", new FakeNode("a"), 0));
        assertThrows(IllegalArgumentException.class, () -> cluster.addNode(" ", new FakeNode(" "), 1));
        assertThrows(IllegalArgumentException.class, () -> cluster.addNode(null, new FakeNode("x"), 1));
        add("a", 1);
        assertThrows(IllegalArgumentException.class, () -> cluster.addNode("a", new FakeNode("a"), 1));
        assertThrows(IllegalArgumentException.class, () -> cluster.removeNode("z"));
        assertEquals("a", cluster.ownerOf("anything"));
        cluster.removeNode("a");
        assertThrows(IllegalStateException.class, () -> cluster.get("k"));
    }
}
