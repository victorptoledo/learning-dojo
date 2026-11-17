package dojo.tasks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

class Stage3WorkersTest {

    static final QueueConfig CONFIG =
            new QueueConfig(Duration.ofMinutes(5), 3, Duration.ofMillis(1), Duration.ofMillis(1));

    private final ManualClock clock = new ManualClock();
    private final TaskQueue queue = new TaskQueue(clock, CONFIG);

    @Test
    void theSameKeyGivesTheSameTask() {
        String first = queue.enqueue("charge", "order=7 amount=1990", "order-7");
        String again = queue.enqueue("charge", "order=7 amount=1990", "order-7");
        assertEquals(first, again);
        assertTrue(queue.poll().isPresent());
        assertTrue(queue.poll().isEmpty(), "only one task was created");
    }

    @Test
    void aKeyStillBindsWhileItsTaskIsLeasedOrDone() {
        String id = queue.enqueue("charge", "p", "k1");
        var delivery = queue.poll().orElseThrow();
        assertEquals(id, queue.enqueue("charge", "p", "k1"));
        queue.ack(delivery);
        assertEquals(id, queue.enqueue("charge", "p", "k1"), "a done task still answers for its key");
    }

    @Test
    void aDeadTaskFreesItsKey() {
        String id = queue.enqueue("charge", "p", "k2");
        for (int i = 0; i < CONFIG.maxAttempts(); i++) {
            queue.nack(queue.poll().orElseThrow(), "declined");
            clock.advance(Duration.ofSeconds(1));
        }
        assertEquals(Status.DEAD, queue.status(id));
        String fresh = queue.enqueue("charge", "p", "k2");
        assertNotEquals(id, fresh);
        assertEquals(Status.READY, queue.status(fresh));
    }

    @Test
    void differentKeysAndNoKeyAreDifferentTasks() {
        String a = queue.enqueue("t", "p", "a");
        String b = queue.enqueue("t", "p", "b");
        String plain = queue.enqueue("t", "p");
        assertEquals(3, Set.of(a, b, plain).size());
    }

    @Test
    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    void eightWorkersFinishEveryTaskExactlyOnce() throws Exception {
        // Real time here, so the 1 ms backoff of the failed first tries passes by itself.
        var queue = new TaskQueue(java.time.Clock.systemUTC(), CONFIG);
        int tasks = 2_000;
        List<String> ids = new ArrayList<>();
        for (int i = 0; i < tasks; i++) {
            ids.add(queue.enqueue("job", Integer.toString(i)));
        }
        var leasedNow = ConcurrentHashMap.<String>newKeySet();
        var finished = ConcurrentHashMap.<String>newKeySet();
        var doubleLeases = new AtomicInteger();
        var start = new CountDownLatch(1);
        ExecutorService workers = Executors.newFixedThreadPool(8);
        List<Future<?>> running = new ArrayList<>();
        for (int w = 0; w < 8; w++) {
            running.add(workers.submit(() -> {
                start.await();
                while (finished.size() < tasks) {
                    var delivery = queue.poll();
                    if (delivery.isEmpty()) {
                        Thread.onSpinWait();
                        continue;
                    }
                    Task task = delivery.get();
                    if (!leasedNow.add(task.id())) {
                        doubleLeases.incrementAndGet();
                    }
                    boolean fail = Integer.parseInt(task.payload()) % 10 == 0 && task.attempt() == 1;
                    leasedNow.remove(task.id());
                    if (fail) {
                        queue.nack(task, "first try fails");
                    } else if (queue.ack(task)) {
                        assertTrue(finished.add(task.id()), "acked twice: " + task.id());
                    }
                }
                return null;
            }));
        }
        start.countDown();
        for (Future<?> worker : running) {
            worker.get();
        }
        workers.shutdown();
        assertEquals(0, doubleLeases.get(), "a task was leased to two workers at once");
        assertEquals(tasks, finished.size());
        for (String id : ids) {
            assertEquals(Status.DONE, queue.status(id));
        }
    }

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void concurrentProducersWithTheSameKeyCreateOneTask() throws Exception {
        ExecutorService producers = Executors.newFixedThreadPool(8);
        var start = new CountDownLatch(1);
        List<Future<String>> ids = new ArrayList<>();
        for (int i = 0; i < 64; i++) {
            ids.add(producers.submit(() -> {
                start.await();
                return queue.enqueue("charge", "p", "same-key");
            }));
        }
        start.countDown();
        Set<String> distinct = ConcurrentHashMap.newKeySet();
        for (Future<String> id : ids) {
            distinct.add(id.get());
        }
        producers.shutdown();
        assertEquals(1, distinct.size());
    }
}
