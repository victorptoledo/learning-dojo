package dojo.broker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dojo.broker.MemoryStore.Crash;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.Test;

class Stage4DeadLettersTest {
    private static final Duration RETENTION = Duration.ofHours(6);

    private final ManualClock clock = new ManualClock();
    private final Broker broker = new Broker(clock, new BrokerConfig(Duration.ofSeconds(10), 3));
    private final MemoryStore store = new MemoryStore();
    private final Map<String, Integer> calls = new HashMap<>();

    /** Credits `account=amount`; a value of `account=poison` always fails. */
    private Map<String, String> handler(Message message, Store s) {
        calls.merge(message.value(), 1, Integer::sum);
        if (message.value().endsWith("=poison")) {
            throw new IllegalArgumentException("not a number: " + message.value());
        }
        return MemoryStore.credit(message, s);
    }

    private Processor processor() {
        return new Processor(broker, "billing", "payments", "worker-1", store, this::handler);
    }

    private int drain(Processor processor) {
        int applied = 0;
        for (int i = 0; i < 50; i++) {
            int n = processor.runOnce(10);
            applied += n;
            if (n == 0 && broker.lag("billing") == 0) {
                break;
            }
        }
        return applied;
    }

    @Test
    void aPoisonMessageIsTriedThenDeadLetteredAndThePartitionMovesOn() {
        broker.createTopic("payments", 1, RETENTION);
        broker.send("payments", "acc-1", "acc-1=10");
        broker.send("payments", "acc-1", "acc-1=poison");
        broker.send("payments", "acc-1", "acc-1=5");
        assertEquals(2, processor().runOnce(10), "the poison message is not counted");
        assertEquals(3, calls.get("acc-1=poison"), "maxAttempts calls in all");
        assertEquals(15, store.balance("acc-1"), "the message behind it was applied");
        assertEquals("3", store.data.get("offset:payments:0"));
        assertEquals(1, broker.partitions("payments.dlq"));
        assertEquals(RETENTION, broker.retention("payments.dlq"));
        List<Message> dead = broker.read("payments.dlq", 0, 0, 10);
        assertEquals(1, dead.size());
        assertEquals("acc-1", dead.get(0).key());
        assertEquals("acc-1=poison", dead.get(0).value());
    }

    @Test
    void aMessageThatFailsOnceIsRetriedAndApplied() {
        broker.createTopic("payments", 1, RETENTION);
        broker.send("payments", "acc-1", "acc-1=7");
        int[] failures = {2};
        var flaky = new Processor(broker, "billing", "payments", "worker-1", store, (m, s) -> {
            if (failures[0]-- > 0) {
                throw new IllegalStateException("database timeout");
            }
            return MemoryStore.credit(m, s);
        });
        assertEquals(1, flaky.runOnce(10));
        assertEquals(7, store.balance("acc-1"));
        assertThrows(NoSuchElementException.class, () -> broker.partitions("payments.dlq"), "no dead letter");
    }

    @Test
    void aPoisonMessageIsHandledOnlyOnceEvenAfterARestart() {
        broker.createTopic("payments", 1, RETENTION);
        broker.send("payments", "acc-1", "acc-1=poison");
        broker.send("payments", "acc-1", "acc-1=1");
        processor().runOnce(10);
        clock.advance(Duration.ofSeconds(10));
        var again = new Processor(broker, "billing", "payments", "worker-2", store, this::handler);
        assertEquals(0, drain(again));
        assertEquals(3, calls.get("acc-1=poison"));
        assertEquals(1, broker.endOffset("payments.dlq", 0));
    }

    @Test
    void anErrorIsACrashNotAFailedMessage() {
        broker.createTopic("payments", 1, RETENTION);
        broker.send("payments", "acc-1", "acc-1=1");
        int[] calls = {0};
        var dying = new Processor(broker, "billing", "payments", "worker-1", store, (m, s) -> {
            calls[0]++;
            throw new Crash("out of memory");
        });
        assertThrows(Crash.class, () -> dying.runOnce(10));
        assertEquals(1, calls[0], "an Error is not retried");
        assertThrows(NoSuchElementException.class, () -> broker.partitions("payments.dlq"));
    }

    @Test
    void poisonInOnePartitionNeverStopsTheOthers() {
        broker.createTopic("payments", 4, RETENTION);
        Map<String, Long> expected = new HashMap<>();
        for (int i = 0; i < 80; i++) {
            String account = "acc-" + (i % 8);
            if (i % 10 == 3) {
                broker.send("payments", account, account + "=poison");
            } else {
                broker.send("payments", account, account + "=" + i);
                expected.merge(account, (long) i, Long::sum);
            }
        }
        assertEquals(72, drain(processor()));
        expected.forEach((account, total) -> assertEquals(total, store.balance(account), account));
        long dead = 0;
        for (int p = 0; p < 4; p++) {
            dead += broker.endOffset("payments.dlq", p);
        }
        assertEquals(8, dead);
    }

    @Test
    void lagIsWhatTheGroupHasNotCommitted() {
        broker.createTopic("payments", 2, RETENTION);
        for (int i = 0; i < 10; i++) {
            broker.send("payments", null, "acc-" + i + "=1");
        }
        broker.join("billing", "payments", "worker-1");
        assertEquals(10, broker.lag("billing"));
        Batch batch = broker.poll("billing", "worker-1", 3); // 3 from partition 0
        assertEquals(10, broker.lag("billing"), "read is not committed");
        broker.commit("billing", "worker-1", batch.generation(), 0, 3);
        assertEquals(7, broker.lag("billing"));
        broker.send("payments", null, "acc-x=1");
        assertEquals(8, broker.lag("billing"));
        assertThrows(NoSuchElementException.class, () -> broker.lag("nobody"));
    }

    @Test
    void lagNeverCountsWhatRetentionDeleted() {
        broker.createTopic("payments", 1, Duration.ofHours(1));
        for (int i = 0; i < 5; i++) {
            broker.send("payments", "k", "acc=1");
        }
        broker.join("billing", "payments", "worker-1");
        clock.advance(Duration.ofHours(1));
        broker.enforceRetention();
        broker.send("payments", "k", "acc=1");
        assertEquals(1, broker.lag("billing"));
    }
}
