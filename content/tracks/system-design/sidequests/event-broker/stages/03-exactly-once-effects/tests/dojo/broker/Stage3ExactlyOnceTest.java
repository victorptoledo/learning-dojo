package dojo.broker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dojo.broker.MemoryStore.Crash;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class Stage3ExactlyOnceTest {
    private static final Duration SESSION = Duration.ofSeconds(10);

    private final ManualClock clock = new ManualClock();
    private final Broker broker = new Broker(clock, new BrokerConfig(SESSION, 3));
    private final MemoryStore store = new MemoryStore();

    /** 60 payments over 6 accounts and 3 partitions; returns the expected balances. */
    private Map<String, Long> payments() {
        broker.createTopic("payments", 3, Duration.ofDays(1));
        Map<String, Long> expected = new HashMap<>();
        for (int i = 0; i < 60; i++) {
            String account = "acc-" + (i % 6);
            long amount = 10 + i;
            broker.send("payments", account, account + "=" + amount);
            expected.merge(account, amount, Long::sum);
        }
        return expected;
    }

    private void assertBalances(Map<String, Long> expected) {
        expected.forEach((account, total) -> assertEquals(total, store.balance(account), account));
    }

    private int drain(Processor processor) {
        int applied = 0;
        for (int i = 0; i < 100; i++) {
            int n = processor.runOnce(7);
            applied += n;
            if (n == 0 && i > 0) {
                break;
            }
        }
        return applied;
    }

    // ---- idempotent producer ----

    @Test
    void aRetriedSendIsWrittenOnce() {
        broker.createTopic("t", 2, Duration.ofDays(1));
        Sent first = broker.send("t", "k", "hello", "producer-1", 0);
        assertEquals(first, broker.send("t", "k", "hello", "producer-1", 0), "the retry gets the first answer");
        Sent second = broker.send("t", "k", "again", "producer-1", 1);
        assertEquals(first, broker.send("t", "other-key", "late retry", "producer-1", 0));
        assertEquals(2, broker.endOffset("t", first.partition()));
        assertEquals(first.offset() + 1, second.offset());
        assertEquals(List.of("hello", "again"),
                broker.read("t", first.partition(), 0, 10).stream().map(Message::value).toList());
    }

    @Test
    void aGapInTheSequenceIsRefused() {
        broker.createTopic("t", 1, Duration.ofDays(1));
        assertThrows(OutOfOrderSequenceException.class, () -> broker.send("t", "k", "v", "p", 1),
                "a producer starts at 0");
        broker.send("t", "k", "v0", "p", 0);
        assertThrows(OutOfOrderSequenceException.class, () -> broker.send("t", "k", "v2", "p", 2));
        assertEquals(1, broker.endOffset("t", 0), "nothing appended");
        broker.send("t", "k", "v1", "p", 1);
        broker.send("t", "k", "q0", "another-producer", 0);
        assertEquals(3, broker.endOffset("t", 0));
    }

    @Test
    void oneSequencePerProducerAcrossTopics() {
        broker.createTopic("a", 1, Duration.ofDays(1));
        broker.createTopic("b", 1, Duration.ofDays(1));
        broker.send("a", "k", "1", "p", 0);
        broker.send("b", "k", "2", "p", 1);
        assertEquals(new Sent(0, 0), broker.send("b", "k", "retry", "p", 0), "sequence 0 went to topic a");
        assertEquals(1, broker.endOffset("b", 0));
    }

    // ---- seek ----

    @Test
    void seekMovesTheMembersPosition() {
        broker.createTopic("t", 2, Duration.ofDays(1));
        for (int i = 0; i < 10; i++) {
            broker.send("t", null, "m" + i);
        }
        broker.join("g", "t", "a");
        broker.join("g", "t", "b"); // a: 0, b: 1
        broker.seek("g", "a", 0, 3);
        assertEquals(List.of("m6", "m8"), broker.poll("g", "a", 10).messages().stream().map(Message::value).toList());
        assertThrows(IllegalStateException.class, () -> broker.seek("g", "a", 1, 0));
    }

    // ---- processor ----

    @Test
    void everyMessageIsAppliedOnceWithItsOffset() {
        Map<String, Long> expected = payments();
        var processor = new Processor(broker, "billing", "payments", "worker-1", store, MemoryStore::credit);
        assertEquals(60, drain(processor));
        assertBalances(expected);
        for (int p = 0; p < 3; p++) {
            assertEquals(Long.toString(broker.endOffset("payments", p)), store.data.get("offset:payments:" + p));
            assertEquals(broker.endOffset("payments", p), broker.committed("billing", p), "the broker offsets follow");
        }
        assertEquals(60, store.transactions, "one transaction per message: effects and offset together");
        assertEquals(0, processor.runOnce(10));
    }

    @Test
    void aCrashAfterSomeEffectsNeverAppliesThemTwice() {
        Map<String, Long> expected = payments();
        int[] calls = {0};
        Handler crashing = (message, s) -> {
            if (++calls[0] == 5) {
                throw new Crash("power cut");
            }
            return MemoryStore.credit(message, s);
        };
        var first = new Processor(broker, "billing", "payments", "worker-1", store, crashing);
        assertThrows(Crash.class, () -> first.runOnce(7), "4 effects applied, nothing committed to the broker");
        for (int p = 0; p < 3; p++) {
            assertEquals(-1, broker.committed("billing", p), "the crash came before the commit");
        }
        clock.advance(SESSION); // worker-1 is dead
        var second = new Processor(broker, "billing", "payments", "worker-2", store, MemoryStore::credit);
        assertEquals(56, drain(second), "the 4 applied before the crash are skipped");
        assertBalances(expected);
    }

    @Test
    void aZombieThatCrashedBeforeCommittingNeverDoublesAnything() {
        Map<String, Long> expected = payments();
        var slow = new Processor(broker, "billing", "payments", "worker-1", store, MemoryStore::credit);
        assertTrue(slow.runOnce(20) > 0);
        clock.advance(SESSION); // worker-1 stops answering heartbeats and loses its partitions
        var fresh = new Processor(broker, "billing", "payments", "worker-2", store, MemoryStore::credit);
        drain(fresh);
        assertBalances(expected);
        assertEquals(0, slow.runOnce(20), "the zombie joins again and finds nothing new");
        assertBalances(expected);
    }

    @Test
    void twoProcessorsShareThePartitionsAndTheEffects() {
        Map<String, Long> expected = payments();
        var a = new Processor(broker, "billing", "payments", "worker-a", store, MemoryStore::credit);
        var b = new Processor(broker, "billing", "payments", "worker-b", store, MemoryStore::credit);
        int applied = 0;
        for (int round = 0; round < 30; round++) {
            applied += a.runOnce(5);
            applied += b.runOnce(5);
        }
        assertEquals(60, applied);
        assertBalances(expected);
    }

    @Test
    void theStoreWinsOverAnOlderBrokerCommit() {
        Map<String, Long> expected = payments();
        var first = new Processor(broker, "billing", "payments", "worker-1", store, MemoryStore::credit);
        drain(first);
        // the broker lost its commits (a new group name has none), the store has the truth
        var other = new Processor(broker, "billing-v2", "payments", "worker-1", store, MemoryStore::credit);
        assertEquals(0, drain(other));
        assertBalances(expected);
    }
}
