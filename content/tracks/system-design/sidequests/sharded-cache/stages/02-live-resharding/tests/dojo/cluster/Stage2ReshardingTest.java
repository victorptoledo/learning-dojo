package dojo.cluster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dojo.cluster.Fakes.FakeNode;
import dojo.cluster.Fakes.FakeOrigin;
import dojo.cluster.Fakes.ManualClock;
import dojo.cluster.Fakes.TestRing;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class Stage2ReshardingTest {
    private static final int KEYS = 4_000;

    private final ManualClock clock = new ManualClock();
    private final FakeOrigin origin = new FakeOrigin();
    private final CacheCluster cluster = new CacheCluster(origin, clock, Fakes.config());
    private final Map<String, FakeNode> nodes = new LinkedHashMap<>();
    private final FakeNode fresh = new FakeNode("e");
    private final TestRing after =
            new TestRing(100, Map.of("a", 1, "b", 1, "c", 1, "d", 1, "e", 1));
    private final List<String> keys = Fakes.keys(KEYS);

    /** Four nodes, every key cached, counters reset. */
    private void warm() {
        for (String name : List.of("a", "b", "c", "d")) {
            var node = new FakeNode(name);
            nodes.put(name, node);
            cluster.addNode(name, node, 1);
        }
        for (String key : keys) {
            cluster.get(key);
        }
        origin.loads.set(0);
        nodes.values().forEach(FakeNode::resetCounts);
    }

    private long movingToE() {
        return keys.stream().filter(k -> after.owner(k).equals("e")).count();
    }

    @Test
    void readsDuringTheMigrationNeverReachTheDatabase() {
        warm();
        cluster.joinNode("e", fresh, 1);
        assertTrue(cluster.migrating());
        for (String key : keys) {
            assertEquals(after.owner(key), cluster.ownerOf(key));
            assertEquals("value-of-" + key, cluster.get(key));
        }
        assertEquals(0, origin.loads.get(), "no stampede: moved keys come from their previous owners");
        assertEquals(movingToE(), fresh.data.size());
        for (var node : nodes.values()) {
            for (String key : node.data.keySet()) {
                assertEquals(node.name, after.owner(key), "a moved key leaves its old node");
            }
        }
        int total = nodes.values().stream().mapToInt(n -> n.data.size()).sum() + fresh.data.size();
        assertEquals(KEYS, total, "nothing lost, nothing duplicated");
    }

    @Test
    void stepsMoveKeysWithinTheBudgetAndScanEachNodeOnce() {
        warm();
        cluster.joinNode("e", fresh, 1);
        int moved = 0;
        int steps = 0;
        while (cluster.migrating()) {
            int step = cluster.migrateStep(50);
            assertTrue(step >= 0 && step <= 50, "a step moves at most its budget: " + step);
            moved += step;
            assertTrue(++steps < 1_000, "the migration ends");
        }
        assertEquals(movingToE(), moved);
        assertEquals(movingToE(), fresh.data.size());
        assertTrue(steps >= movingToE() / 50, "the budget was used: " + steps + " steps");
        for (var node : nodes.values()) {
            assertEquals(1, node.scans.get(), node.name + " is scanned once per migration");
        }
        nodes.values().forEach(FakeNode::resetCounts);
        for (String key : keys) {
            assertEquals("value-of-" + key, cluster.get(key));
        }
        assertEquals(0, origin.loads.get());
        long askedOld = nodes.values().stream().mapToInt(n -> n.gets.get()).sum();
        assertEquals(KEYS - movingToE(), askedOld, "after the migration a moved key is only asked to e");
        assertEquals(0, cluster.migrateStep(10), "no migration, nothing to move");
    }

    @Test
    void keysMovedByReadsAreNotMovedTwice() {
        warm();
        cluster.joinNode("e", fresh, 1);
        for (String key : keys.subList(0, KEYS / 2)) {
            cluster.get(key);
        }
        long movedByReads = keys.subList(0, KEYS / 2).stream().filter(k -> after.owner(k).equals("e")).count();
        int moved = 0;
        while (cluster.migrating()) {
            moved += cluster.migrateStep(1_000);
        }
        assertEquals(movingToE() - movedByReads, moved);
        assertEquals(movingToE(), fresh.data.size());
        assertEquals(0, origin.loads.get());
    }

    @Test
    void keysThatStayAreLeftAlone() {
        warm();
        cluster.joinNode("e", fresh, 1);
        while (cluster.migrating()) {
            cluster.migrateStep(500);
        }
        int puts = nodes.values().stream().mapToInt(n -> n.puts.get()).sum();
        int removes = nodes.values().stream().mapToInt(n -> n.removes.get()).sum();
        assertEquals(0, puts, "nothing is written to the old nodes");
        assertEquals(movingToE(), removes, "only moved keys are removed");
    }

    @Test
    void invalidateDuringTheMigrationAlsoClearsThePreviousOwner() {
        warm();
        String key = keys.stream().filter(k -> after.owner(k).equals("e")).findFirst().orElseThrow();
        cluster.joinNode("e", fresh, 1);
        origin.values.put(key, "new value");
        cluster.invalidate(key);
        assertEquals("new value", cluster.get(key), "the old value must not come back from the previous owner");
        while (cluster.migrating()) {
            cluster.migrateStep(500);
        }
        assertEquals("new value", fresh.data.get(key));
    }

    @Test
    void newValuesDuringTheMigrationGoToTheNewOwner() {
        warm();
        cluster.joinNode("e", fresh, 1);
        String key = "brand-new";
        int i = 0;
        while (!after.owner(key).equals("e")) {
            key = "brand-new-" + i++;
        }
        assertEquals("value-of-" + key, cluster.get(key));
        assertEquals("value-of-" + key, fresh.data.get(key));
        assertEquals(1, origin.loads.get());
    }

    @Test
    void theRingCannotChangeDuringAMigration() {
        warm();
        cluster.joinNode("e", fresh, 1);
        assertThrows(IllegalStateException.class, () -> cluster.joinNode("f", new FakeNode("f"), 1));
        assertThrows(IllegalStateException.class, () -> cluster.addNode("f", new FakeNode("f"), 1));
        assertThrows(IllegalStateException.class, () -> cluster.removeNode("a"));
        while (cluster.migrating()) {
            cluster.migrateStep(1_000);
        }
        cluster.joinNode("f", new FakeNode("f"), 1);
        assertTrue(cluster.migrating());
    }

    @Test
    void joiningAnEmptyClusterIsJustAnAdd() {
        cluster.joinNode("a", new FakeNode("a"), 1);
        assertFalse(cluster.migrating());
        assertEquals("a", cluster.ownerOf("k"));
        assertThrows(IllegalArgumentException.class, () -> cluster.joinNode("a", new FakeNode("a"), 1));
        assertThrows(IllegalArgumentException.class, () -> cluster.joinNode("b", new FakeNode("b"), 0));
    }
}
