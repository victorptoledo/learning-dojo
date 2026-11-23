package dojo.broker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.Test;

class Stage1LogTest {
    static final BrokerConfig CONFIG = new BrokerConfig(Duration.ofSeconds(10), 3);
    static final Duration DAY = Duration.ofDays(1);

    private final ManualClock clock = new ManualClock();
    private final Broker broker = new Broker(clock, CONFIG);

    static int partitionOf(String key, int partitions) {
        return Math.floorMod(Hashing.hash64(key), partitions);
    }

    @Test
    void aTopicHasPartitionsThatStartEmpty() {
        broker.createTopic("orders", 4, DAY);
        assertEquals(4, broker.partitions("orders"));
        assertSame(CONFIG, broker.config());
        for (int p = 0; p < 4; p++) {
            assertEquals(0, broker.startOffset("orders", p));
            assertEquals(0, broker.endOffset("orders", p));
            assertEquals(List.of(), broker.read("orders", p, 0, 10));
        }
    }

    @Test
    void aKeyAlwaysGoesToTheSamePartitionWithTheNextOffset() {
        broker.createTopic("orders", 4, DAY);
        int p = partitionOf("customer-7", 4);
        assertEquals(new Sent(p, 0), broker.send("orders", "customer-7", "created"));
        assertEquals(new Sent(p, 1), broker.send("orders", "customer-7", "paid"));
        assertEquals(new Sent(p, 2), broker.send("orders", "customer-7", "shipped"));
        assertEquals(3, broker.endOffset("orders", p));
        List<Message> log = broker.read("orders", p, 0, 10);
        assertEquals(List.of("created", "paid", "shipped"), log.stream().map(Message::value).toList());
        assertEquals(new Message("orders", p, 1, "customer-7", "paid", clock.instant()), log.get(1));
    }

    @Test
    void keysSpreadOverThePartitionsAndKeepTheirOrder() {
        broker.createTopic("events", 8, DAY);
        for (int i = 0; i < 400; i++) {
            String key = "user-" + (i % 40);
            Sent sent = broker.send("events", key, key + ":" + i);
            assertEquals(partitionOf(key, 8), sent.partition());
        }
        long total = 0;
        for (int p = 0; p < 8; p++) {
            total += broker.endOffset("events", p);
            assertTrue(broker.endOffset("events", p) > 0, "every partition gets keys");
            List<Message> log = broker.read("events", p, 0, 1_000);
            for (int i = 0; i < log.size(); i++) {
                assertEquals(i, log.get(i).offset());
            }
        }
        assertEquals(400, total);
        int p = partitionOf("user-3", 8);
        List<Integer> order = new ArrayList<>();
        for (Message m : broker.read("events", p, 0, 1_000)) {
            if (m.key().equals("user-3")) {
                order.add(Integer.parseInt(m.value().split(":")[1]));
            }
        }
        assertEquals(List.of(3, 43, 83, 123, 163, 203, 243, 283, 323, 363), order);
    }

    @Test
    void messagesWithoutAKeyGoRoundThePartitions() {
        broker.createTopic("logs", 3, DAY);
        broker.createTopic("other", 2, DAY);
        List<Integer> partitions = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            partitions.add(broker.send("logs", null, "line " + i).partition());
        }
        assertEquals(List.of(0, 1, 2, 0, 1, 2, 0), partitions);
        assertEquals(0, broker.send("other", null, "x").partition(), "each topic has its own turn");
        assertEquals(null, broker.read("logs", 0, 0, 1).get(0).key());
    }

    @Test
    void readsReturnAtMostMaxFromTheOffset() {
        broker.createTopic("t", 1, DAY);
        for (int i = 0; i < 10; i++) {
            broker.send("t", "k", "v" + i);
        }
        assertEquals(List.of("v4", "v5", "v6"), broker.read("t", 0, 4, 3).stream().map(Message::value).toList());
        assertEquals(List.of("v8", "v9"), broker.read("t", 0, 8, 5).stream().map(Message::value).toList());
        assertEquals(List.of(), broker.read("t", 0, 10, 5), "at the end: nothing new yet");
        assertThrows(OffsetOutOfRangeException.class, () -> broker.read("t", 0, 11, 5));
        assertThrows(OffsetOutOfRangeException.class, () -> broker.read("t", 0, -1, 5));
    }

    @Test
    void retentionCutsTheOldestMessagesAndKeepsTheOffsets() {
        broker.createTopic("t", 2, Duration.ofHours(1));
        for (int i = 0; i < 5; i++) {
            broker.send("t", null, "old" + i);
        }
        clock.advance(Duration.ofMinutes(30));
        for (int i = 0; i < 4; i++) {
            broker.send("t", null, "new" + i);
        }
        clock.advance(Duration.ofMinutes(30).minusSeconds(1));
        assertEquals(0, broker.enforceRetention(), "one second too early");
        clock.advance(Duration.ofSeconds(1));
        assertEquals(5, broker.enforceRetention());
        assertEquals(3, broker.startOffset("t", 0));
        assertEquals(5, broker.endOffset("t", 0));
        assertEquals(List.of("new1", "new3"), broker.read("t", 0, 3, 10).stream().map(Message::value).toList());
        assertEquals(3, broker.read("t", 0, 3, 10).get(0).offset());
        assertThrows(OffsetOutOfRangeException.class, () -> broker.read("t", 0, 2, 10));
        assertEquals(new Sent(1, 4), broker.send("t", null, "after"), "offsets are never reused");
        clock.advance(Duration.ofHours(2));
        assertEquals(5, broker.enforceRetention());
        assertEquals(5, broker.startOffset("t", 0));
        assertEquals(5, broker.startOffset("t", 1));
        assertEquals(List.of(), broker.read("t", 1, 5, 10));
    }

    @Test
    void retentionIsPerTopic() {
        broker.createTopic("short", 1, Duration.ofMinutes(1));
        broker.createTopic("long", 1, DAY);
        broker.send("short", "k", "a");
        broker.send("long", "k", "a");
        clock.advance(Duration.ofHours(1));
        assertEquals(1, broker.enforceRetention());
        assertEquals(1, broker.startOffset("short", 0));
        assertEquals(0, broker.startOffset("long", 0));
    }

    @Test
    void badTopicsAndUnknownPartitionsAreRefused() {
        broker.createTopic("t", 2, DAY);
        assertThrows(IllegalArgumentException.class, () -> broker.createTopic("t", 2, DAY));
        assertThrows(IllegalArgumentException.class, () -> broker.createTopic("x", 0, DAY));
        assertThrows(IllegalArgumentException.class, () -> broker.createTopic(" ", 1, DAY));
        assertThrows(IllegalArgumentException.class, () -> broker.createTopic("y", 1, Duration.ZERO));
        assertThrows(NoSuchElementException.class, () -> broker.send("nope", "k", "v"));
        assertThrows(NoSuchElementException.class, () -> broker.partitions("nope"));
        assertThrows(NoSuchElementException.class, () -> broker.read("t", 2, 0, 1));
        assertThrows(NoSuchElementException.class, () -> broker.endOffset("t", -1));
    }
}
