package dojo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class LoadSheddingQueueGradingTest {
    private final TestClock clock = new TestClock();

    private static List<String> drain(LoadSheddingQueue queue) {
        var jobs = new ArrayList<String>();
        for (Optional<String> job = queue.poll(); job.isPresent(); job = queue.poll()) {
            jobs.add(job.get());
        }
        return jobs;
    }

    @Test
    void each_priority_is_first_in_first_out_and_critical_goes_first() {
        var queue = new LoadSheddingQueue(10, 10, Duration.ofSeconds(5), clock);
        queue.offer("s1", Priority.SHEDDABLE);
        queue.offer("c1", Priority.CRITICAL);
        queue.offer("s2", Priority.SHEDDABLE);
        queue.offer("c2", Priority.CRITICAL);
        assertEquals(List.of("c1", "c2", "s1", "s2"), drain(queue));
        assertEquals(Optional.empty(), queue.poll());
    }

    @Test
    void sheddable_work_is_refused_past_its_limit_but_critical_work_is_not() {
        var queue = new LoadSheddingQueue(4, 2, Duration.ofSeconds(5), clock);
        assertTrue(queue.offer("s1", Priority.SHEDDABLE));
        assertTrue(queue.offer("s2", Priority.SHEDDABLE));
        assertFalse(queue.offer("s3", Priority.SHEDDABLE));
        assertTrue(queue.offer("c1", Priority.CRITICAL));
        assertTrue(queue.offer("c2", Priority.CRITICAL));
        assertEquals(4, queue.size());
        assertFalse(queue.offer("s4", Priority.SHEDDABLE), "the limit counts jobs of any priority");
        assertEquals(new QueueStats(4, 2, 0, 0, 0), queue.stats());
    }

    @Test
    void a_critical_job_pushes_out_the_newest_sheddable_job() {
        var queue = new LoadSheddingQueue(3, 3, Duration.ofSeconds(5), clock);
        queue.offer("s1", Priority.SHEDDABLE);
        queue.offer("s2", Priority.SHEDDABLE);
        queue.offer("s3", Priority.SHEDDABLE);
        assertTrue(queue.offer("c1", Priority.CRITICAL));
        assertEquals(1, queue.stats().shed());
        assertEquals(List.of("c1", "s1", "s2"), drain(queue));
    }

    @Test
    void a_queue_full_of_critical_work_rejects_more() {
        var queue = new LoadSheddingQueue(2, 1, Duration.ofSeconds(5), clock);
        assertTrue(queue.offer("c1", Priority.CRITICAL));
        assertTrue(queue.offer("c2", Priority.CRITICAL));
        assertFalse(queue.offer("c3", Priority.CRITICAL));
        assertEquals(new QueueStats(2, 1, 0, 0, 0), queue.stats());
    }

    @Test
    void jobs_that_waited_too_long_are_dropped_not_served() {
        var queue = new LoadSheddingQueue(10, 10, Duration.ofSeconds(2), clock);
        queue.offer("old", Priority.CRITICAL);
        clock.advance(Duration.ofMillis(1500));
        queue.offer("fresh", Priority.CRITICAL);
        queue.offer("fresh-s", Priority.SHEDDABLE);
        clock.advance(Duration.ofMillis(500));
        assertEquals(Optional.of("fresh"), queue.poll(), "old waited exactly 2 s and is expired");
        assertEquals(Optional.of("fresh-s"), queue.poll());
        assertEquals(new QueueStats(3, 0, 0, 1, 2), queue.stats());
    }

    @Test
    void expired_jobs_free_room_for_new_work() {
        var queue = new LoadSheddingQueue(2, 2, Duration.ofSeconds(1), clock);
        queue.offer("a", Priority.CRITICAL);
        queue.offer("b", Priority.SHEDDABLE);
        assertFalse(queue.offer("c", Priority.SHEDDABLE));
        clock.advance(Duration.ofSeconds(1));
        assertEquals(0, queue.size());
        assertTrue(queue.offer("c", Priority.SHEDDABLE));
        assertEquals(2, queue.stats().expired());
    }

    @Test
    void a_zero_sheddable_limit_refuses_all_sheddable_work() {
        var queue = new LoadSheddingQueue(5, 0, Duration.ofSeconds(1), clock);
        assertFalse(queue.offer("s", Priority.SHEDDABLE));
        assertTrue(queue.offer("c", Priority.CRITICAL));
    }

    @Test
    void concurrent_offers_never_overfill_the_queue() throws Exception {
        var queue = new LoadSheddingQueue(100, 50, Duration.ofMinutes(1), clock);
        var accepted = new AtomicInteger();
        try (var executor = Executors.newFixedThreadPool(8)) {
            for (int i = 0; i < 1000; i++) {
                String job = "job-" + i;
                executor.submit(() -> {
                    if (queue.offer(job, Priority.CRITICAL)) {
                        accepted.incrementAndGet();
                    }
                });
            }
        }
        assertEquals(100, accepted.get());
        assertEquals(new QueueStats(100, 900, 0, 0, 0), queue.stats());
    }

    @Test
    void impossible_queues_are_refused() {
        assertThrows(IllegalArgumentException.class, () -> new LoadSheddingQueue(0, 0, Duration.ofSeconds(1), clock));
        assertThrows(IllegalArgumentException.class, () -> new LoadSheddingQueue(5, 6, Duration.ofSeconds(1), clock));
        assertThrows(IllegalArgumentException.class, () -> new LoadSheddingQueue(5, -1, Duration.ofSeconds(1), clock));
        assertThrows(IllegalArgumentException.class, () -> new LoadSheddingQueue(5, 2, Duration.ZERO, clock));
    }
}
