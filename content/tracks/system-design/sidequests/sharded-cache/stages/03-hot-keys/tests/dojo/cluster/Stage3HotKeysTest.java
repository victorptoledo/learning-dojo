package dojo.cluster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dojo.cluster.Fakes.FakeNode;
import dojo.cluster.Fakes.FakeOrigin;
import dojo.cluster.Fakes.ManualClock;
import dojo.cluster.Fakes.TestRing;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class Stage3HotKeysTest {
    private static final String KEY = "celebrity:1";

    private final ManualClock clock = new ManualClock();
    private final FakeOrigin origin = new FakeOrigin();
    private final Map<String, FakeNode> nodes = new LinkedHashMap<>();
    private final ExecutorService pool = Executors.newFixedThreadPool(8);

    @AfterEach
    void stopThreads() {
        origin.gates.values().forEach(CountDownLatch::countDown);
        pool.shutdownNow();
    }

    private CacheCluster cluster(int nodeCount, int threshold, int replicas) {
        var base = Fakes.config();
        var config = new ClusterConfig(base.virtualNodes(), Duration.ofSeconds(1), threshold, replicas,
                base.failureThreshold(), base.ejectFor(), base.originConcurrency());
        var cluster = new CacheCluster(origin, clock, config);
        for (int i = 0; i < nodeCount; i++) {
            String name = "node-" + (char) ('a' + i);
            var node = new FakeNode(name);
            nodes.put(name, node);
            cluster.addNode(name, node, 1);
        }
        return cluster;
    }

    private List<String> copies(int count) {
        Map<String, Integer> weights = new LinkedHashMap<>();
        nodes.keySet().forEach(n -> weights.put(n, 1));
        return new TestRing(100, weights).walk(KEY, count);
    }

    private void read(CacheCluster cluster, int times) {
        for (int i = 0; i < times; i++) {
            assertEquals("value-of-" + KEY, cluster.get(KEY));
        }
    }

    private void resetCounts() {
        nodes.values().forEach(FakeNode::resetCounts);
    }

    private int gets(String node) {
        return nodes.get(node).gets.get();
    }

    @Test
    void aHotKeyIsCopiedAndReadsGoRoundTheCopies() {
        var cluster = cluster(5, 10, 3);
        List<String> copies = copies(3);
        read(cluster, 10);
        assertEquals(10, gets(copies.get(0)), "reads up to the threshold go to the owner");
        assertEquals("value-of-" + KEY, nodes.get(copies.get(1)).data.get(KEY), "copied when it became hot");
        assertEquals("value-of-" + KEY, nodes.get(copies.get(2)).data.get(KEY));
        resetCounts();
        read(cluster, 30);
        for (String copy : copies) {
            assertEquals(10, gets(copy), copy + " serves a third of the reads");
        }
        assertEquals(1, origin.loads.get());
        for (var node : nodes.values()) {
            if (!copies.contains(node.name)) {
                assertEquals(0, node.calls.get(), "no other node is involved");
            }
        }
    }

    @Test
    void theRotationFollowsTheRingOrder() {
        var cluster = cluster(5, 3, 3);
        List<String> copies = copies(3);
        read(cluster, 3);
        for (int i = 0; i < 6; i++) {
            resetCounts();
            read(cluster, 1);
            assertEquals(1, gets(copies.get(i % 3)), "read " + i + " goes to copy " + i % 3);
        }
    }

    @Test
    void belowTheThresholdNothingIsCopied() {
        var cluster = cluster(5, 10, 3);
        List<String> copies = copies(3);
        read(cluster, 9);
        clock.advance(Duration.ofSeconds(1));
        read(cluster, 9);
        assertEquals(18, gets(copies.get(0)));
        assertEquals(0, nodes.get(copies.get(1)).calls.get());
        assertEquals(0, nodes.get(copies.get(2)).calls.get());
    }

    @Test
    void windowsAreAlignedOnTheClock() {
        var cluster = cluster(5, 10, 3);
        List<String> copies = copies(3);
        clock.advance(Duration.ofMillis(900));
        read(cluster, 5);
        clock.advance(Duration.ofMillis(200)); // 12:00:01.100: a new window
        read(cluster, 5);
        assertEquals(0, nodes.get(copies.get(1)).calls.get(), "5 + 5 reads in two windows are not hot");
        read(cluster, 5);
        assertEquals(1, nodes.get(copies.get(1)).puts.get(), "10 in this window are");
    }

    @Test
    void aKeyStaysHotUntilTheEndOfTheNextWindow() {
        var cluster = cluster(5, 10, 3);
        List<String> copies = copies(3);
        read(cluster, 10);
        clock.advance(Duration.ofMillis(1_999)); // the last instant of the next window
        resetCounts();
        read(cluster, 3);
        assertEquals(1, gets(copies.get(1)), "still hot");
        clock.advance(Duration.ofMillis(1));
        resetCounts();
        read(cluster, 3);
        assertEquals(3, gets(copies.get(0)), "cold again: every read goes to the owner");
        assertEquals(0, gets(copies.get(1)));
    }

    @Test
    void aKeyThatIsStillReadALotStaysHot() {
        var cluster = cluster(5, 10, 3);
        List<String> copies = copies(3);
        read(cluster, 10);
        clock.advance(Duration.ofSeconds(1));
        read(cluster, 10);
        clock.advance(Duration.ofSeconds(1));
        resetCounts();
        read(cluster, 3);
        assertEquals(1, gets(copies.get(2)), "hot again in the next window");
    }

    @Test
    void invalidatingAHotKeyRemovesEveryCopy() {
        var cluster = cluster(5, 10, 3);
        List<String> copies = copies(3);
        read(cluster, 10);
        origin.values.put(KEY, "edited");
        cluster.invalidate(KEY);
        for (String copy : copies) {
            assertFalse(nodes.get(copy).data.containsKey(KEY), copy + " still has the old value");
        }
        for (int i = 0; i < 6; i++) {
            assertEquals("edited", cluster.get(KEY));
        }
        assertEquals(2, origin.loadsOf(KEY), "a copy that misses is refilled from the owner");
    }

    @Test
    void aSmallClusterHasFewerCopies() {
        var cluster = cluster(2, 4, 3);
        read(cluster, 4);
        resetCounts();
        read(cluster, 10);
        assertEquals(5, gets("node-a"));
        assertEquals(5, gets("node-b"));
    }

    @Test
    void concurrentMissesOnOneKeyLoadItOnce() throws Exception {
        var cluster = cluster(3, Integer.MAX_VALUE, 1);
        var gate = new CountDownLatch(1);
        var loading = origin.gate("slow", gate);
        List<Future<String>> results = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            results.add(pool.submit(() -> cluster.get("slow")));
        }
        assertTrue(loading.await(10, TimeUnit.SECONDS), "a load started");
        Thread.sleep(300); // let the other readers arrive and wait
        gate.countDown();
        for (Future<String> result : results) {
            assertEquals("value-of-slow", result.get(10, TimeUnit.SECONDS));
        }
        assertEquals(1, origin.loadsOf("slow"), "one load for eight concurrent misses");
    }

    @Test
    void aFailedLoadFailsItsWaitersAndTheNextReadTriesAgain() throws Exception {
        var cluster = cluster(3, Integer.MAX_VALUE, 1);
        origin.failing.add("broken");
        var gate = new CountDownLatch(1);
        var loading = origin.gate("broken", gate);
        List<Future<String>> results = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            results.add(pool.submit(() -> cluster.get("broken")));
        }
        assertTrue(loading.await(10, TimeUnit.SECONDS));
        Thread.sleep(300);
        gate.countDown();
        for (Future<String> result : results) {
            var thrown = assertThrows(ExecutionException.class, () -> result.get(10, TimeUnit.SECONDS));
            assertTrue(thrown.getCause() instanceof RuntimeException, "the database error reaches every caller");
        }
        assertEquals(1, origin.loadsOf("broken"));
        origin.failing.clear();
        assertEquals("value-of-broken", cluster.get("broken"));
        assertEquals(2, origin.loadsOf("broken"));
    }

    @Test
    void aSlowLoadNeverBlocksOtherKeys() throws Exception {
        var cluster = cluster(3, 2, 2);
        var gate = new CountDownLatch(1);
        var loading = origin.gate("slow", gate);
        Future<String> slow = pool.submit(() -> cluster.get("slow"));
        assertTrue(loading.await(10, TimeUnit.SECONDS));
        Future<String> other = pool.submit(() -> {
            for (int i = 0; i < 5; i++) {
                cluster.get("fast");
            }
            cluster.invalidate("fast");
            return cluster.get("fast") + " " + cluster.ownerOf("fast");
        });
        assertTrue(other.get(5, TimeUnit.SECONDS).startsWith("value-of-fast"), "other keys go on while one load waits");
        gate.countDown();
        assertEquals("value-of-slow", slow.get(10, TimeUnit.SECONDS));
    }

    @Test
    void manyThreadsReadingHotAndColdKeysAgree() throws Exception {
        var cluster = cluster(5, 50, 3);
        List<Future<?>> done = new ArrayList<>();
        var start = new CountDownLatch(1);
        for (int t = 0; t < 8; t++) {
            int thread = t;
            done.add(pool.submit(() -> {
                start.await();
                for (int i = 0; i < 2_000; i++) {
                    String key = i % 2 == 0 ? KEY : "user:" + (thread * 2_000 + i);
                    assertEquals("value-of-" + key, cluster.get(key));
                }
                return null;
            }));
        }
        start.countDown();
        for (Future<?> f : done) {
            f.get(60, TimeUnit.SECONDS);
        }
        assertEquals(1, origin.loadsOf(KEY), "the hot key is loaded once");
        assertEquals(1 + 8 * 1_000, origin.loads.get());
    }
}
