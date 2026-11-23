package dojo.cluster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dojo.cluster.Fakes.FakeNode;
import dojo.cluster.Fakes.FakeOrigin;
import dojo.cluster.Fakes.ManualClock;
import dojo.cluster.Fakes.TestRing;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class Stage4FailuresTest {
    private static final Duration EJECT = Duration.ofSeconds(30);

    private final ManualClock clock = new ManualClock();
    private final FakeOrigin origin = new FakeOrigin();
    private final Map<String, FakeNode> nodes = new LinkedHashMap<>();
    private final ExecutorService pool = Executors.newFixedThreadPool(4);

    @AfterEach
    void stopThreads() {
        origin.gates.values().forEach(CountDownLatch::countDown);
        pool.shutdownNow();
    }

    private CacheCluster cluster(int nodeCount, int hotThreshold, int hotReplicas, int originConcurrency) {
        var config = new ClusterConfig(100, Duration.ofSeconds(1), hotThreshold, hotReplicas, 3, EJECT,
                originConcurrency);
        var cluster = new CacheCluster(origin, clock, config);
        for (int i = 0; i < nodeCount; i++) {
            String name = "node-" + (char) ('a' + i);
            var node = new FakeNode(name);
            nodes.put(name, node);
            cluster.addNode(name, node, 1);
        }
        return cluster;
    }

    private CacheCluster cluster() {
        return cluster(3, Integer.MAX_VALUE, 1, Integer.MAX_VALUE);
    }

    private TestRing ring(List<String> without) {
        Map<String, Integer> weights = new LinkedHashMap<>();
        nodes.keySet().stream().filter(n -> !without.contains(n)).forEach(n -> weights.put(n, 1));
        return new TestRing(100, weights);
    }

    /** Keys owned by the node on the full ring. */
    private List<String> keysOf(String node, int count) {
        TestRing full = ring(List.of());
        return Fakes.keys(5_000).stream().filter(k -> full.owner(k).equals(node)).limit(count).toList();
    }

    @Test
    void aDownOwnerNeverFailsTheRead() {
        var cluster = cluster();
        String key = keysOf("node-a", 1).get(0);
        nodes.get("node-a").down = true;
        assertEquals("value-of-" + key, cluster.get(key));
        assertEquals(1, origin.loads.get());
        for (var node : nodes.values()) {
            assertFalse(node.data.containsKey(key), "not stored anywhere for that read");
        }
    }

    @Test
    void failuresInARowEjectTheNodeAndItsKeysMoveOn() {
        var cluster = cluster();
        var a = nodes.get("node-a");
        List<String> keys = keysOf("node-a", 20);
        a.down = true;
        for (String key : keys.subList(0, 3)) {
            cluster.get(key);
        }
        assertEquals(3, a.calls.get());
        TestRing without = ring(List.of("node-a"));
        for (String key : keys) {
            assertEquals(without.owner(key), cluster.ownerOf(key), "the next node clockwise serves it");
            assertEquals("value-of-" + key, cluster.get(key));
            assertEquals("value-of-" + key, cluster.get(key));
        }
        assertEquals(3, a.calls.get(), "an ejected node gets no calls");
        assertEquals(3 + 20, origin.loads.get(), "after the ejection, the keys are cached on their new owners");
    }

    @Test
    void aSuccessResetsTheFailures() {
        var cluster = cluster();
        var a = nodes.get("node-a");
        List<String> keys = keysOf("node-a", 5);
        a.down = true;
        cluster.get(keys.get(0));
        cluster.get(keys.get(1));
        a.down = false;
        cluster.get(keys.get(2));
        a.down = true;
        cluster.get(keys.get(3));
        cluster.get(keys.get(4));
        assertEquals("node-a", cluster.ownerOf(keys.get(0)), "2 + 2 failures are not 3 in a row");
    }

    @Test
    void aProbeBringsTheNodeBackOrEjectsItAgain() {
        var cluster = cluster();
        var a = nodes.get("node-a");
        String key = keysOf("node-a", 1).get(0);
        a.down = true;
        for (int i = 0; i < 3; i++) {
            cluster.get(key);
        }
        clock.advance(EJECT.minusSeconds(1));
        cluster.get(key);
        assertEquals(3, a.calls.get(), "still ejected");
        clock.advance(Duration.ofSeconds(1));
        cluster.get(key);
        assertEquals(4, a.calls.get(), "the probe");
        assertNotEquals("node-a", cluster.ownerOf(key), "a failed probe ejects it again at once");
        cluster.get(key);
        assertEquals(4, a.calls.get());
        clock.advance(EJECT);
        a.down = false;
        assertEquals("value-of-" + key, cluster.get(key));
        assertEquals("node-a", cluster.ownerOf(key), "a probe that works brings the node back");
        assertEquals("value-of-" + key, cluster.get(key));
        assertEquals("value-of-" + key, a.data.get(key));
    }

    @Test
    void hotCopiesOnAnEjectedNodeAreSkipped() {
        var cluster = cluster(5, 3, 3, Integer.MAX_VALUE);
        String key = "celebrity:1";
        List<String> copies = ring(List.of()).walk(key, 3);
        for (int i = 0; i < 3; i++) {
            cluster.get(key);
        }
        var replica = nodes.get(copies.get(1));
        replica.down = true;
        for (int i = 0; i < 60; i++) {
            assertEquals("value-of-" + key, cluster.get(key));
        }
        assertTrue(replica.calls.get() <= 4, "at most the failures that ejected it: " + replica.calls.get());
        assertEquals(1, origin.loadsOf(key), "a failing copy is a miss, served from the owner");
    }

    @Test
    void aMigrationFinishesWhenAnOldNodeFails() {
        var cluster = cluster(4, Integer.MAX_VALUE, 1, Integer.MAX_VALUE);
        for (String key : Fakes.keys(2_000)) {
            cluster.get(key);
        }
        var fresh = new FakeNode("node-e");
        nodes.put("node-e", fresh);
        cluster.joinNode("node-e", fresh, 1);
        var b = nodes.get("node-b");
        long lost = b.data.keySet().stream().filter(k -> ring(List.of()).owner(k).equals("node-e")).count();
        long moving = Fakes.keys(2_000).stream().filter(k -> ring(List.of()).owner(k).equals("node-e")).count();
        b.down = true;
        int steps = 0;
        while (cluster.migrating()) {
            cluster.migrateStep(100);
            assertTrue(++steps < 500, "the migration ends");
        }
        assertEquals(moving - lost, fresh.data.size());
    }

    @Test
    void theDatabaseGetsAtMostItsConcurrencyAndTheRestIsShed() throws Exception {
        var cluster = cluster(3, Integer.MAX_VALUE, 1, 2);
        var gate = new CountDownLatch(1);
        var first = origin.gate("slow-1", gate);
        var second = origin.gate("slow-2", gate);
        Future<String> one = pool.submit(() -> cluster.get("slow-1"));
        Future<String> two = pool.submit(() -> cluster.get("slow-2"));
        assertTrue(first.await(10, TimeUnit.SECONDS) && second.await(10, TimeUnit.SECONDS));
        assertThrows(OverloadedException.class, () -> cluster.get("slow-3"));
        assertEquals(0, origin.loadsOf("slow-3"), "a shed read never reaches the database");
        Future<String> joined = pool.submit(() -> cluster.get("slow-1"));
        Thread.sleep(200);
        assertFalse(joined.isDone(), "a reader of a running load waits for it, it is not refused");
        gate.countDown();
        assertEquals("value-of-slow-1", one.get(10, TimeUnit.SECONDS));
        assertEquals("value-of-slow-2", two.get(10, TimeUnit.SECONDS));
        assertEquals("value-of-slow-1", joined.get(10, TimeUnit.SECONDS));
        assertEquals("value-of-slow-3", cluster.get("slow-3"), "room again once the loads end");
        assertEquals(2, origin.maxRunning.get());
    }

    @Test
    void cachedKeysAreServedWhileTheDatabaseIsFull() throws Exception {
        var cluster = cluster(3, Integer.MAX_VALUE, 1, 1);
        cluster.get("warm");
        var gate = new CountDownLatch(1);
        var started = origin.gate("slow", gate);
        Future<String> slow = pool.submit(() -> cluster.get("slow"));
        assertTrue(started.await(10, TimeUnit.SECONDS));
        assertEquals("value-of-warm", cluster.get("warm"), "a hit needs no database");
        assertThrows(OverloadedException.class, () -> cluster.get("cold"));
        gate.countDown();
        assertEquals("value-of-slow", slow.get(10, TimeUnit.SECONDS));
    }
}
