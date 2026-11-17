package dojo.tasks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class Stage2BackoffTest {

    static final QueueConfig CONFIG =
            new QueueConfig(Duration.ofMinutes(5), 4, Duration.ofSeconds(1), Duration.ofSeconds(5));

    private final ManualClock clock = new ManualClock();
    private final TaskQueue queue = new TaskQueue(clock, CONFIG);

    private Task pollNow() {
        return queue.poll().orElseThrow(() -> new AssertionError("a task should be ready"));
    }

    @Test
    void eachFailureDoublesTheWaitUpToTheMaximum() {
        var queue = new TaskQueue(clock, new QueueConfig(Duration.ofMinutes(5), 10, Duration.ofSeconds(1),
                Duration.ofSeconds(5)));
        queue.enqueue("t", "p");
        Task delivery = queue.poll().orElseThrow();
        long[] waits = {1, 2, 4, 5, 5};
        for (int i = 0; i < waits.length; i++) {
            assertEquals(i + 1, delivery.attempt());
            queue.nack(delivery, "boom");
            clock.advance(Duration.ofSeconds(waits[i]).minusMillis(1));
            assertEquals(Optional.empty(), queue.poll(), "attempt " + (i + 1) + ": still waiting");
            clock.advance(Duration.ofMillis(1));
            delivery = queue.poll().orElseThrow(() -> new AssertionError("ready once its wait is over"));
        }
    }

    @Test
    void aWaitingTaskIsReadyButNotDelivered() {
        String id = queue.enqueue("t", "p");
        queue.nack(pollNow(), "boom");
        assertEquals(Status.READY, queue.status(id));
        assertEquals(Optional.empty(), queue.poll());
    }

    @Test
    void theLastFailedAttemptMakesTheTaskDead() {
        String id = queue.enqueue("email", "to=ana");
        for (int attempt = 1; attempt <= 3; attempt++) {
            Task delivery = pollNow();
            assertEquals(attempt, delivery.attempt());
            queue.nack(delivery, "smtp error " + attempt);
            clock.advance(Duration.ofMinutes(1));
        }
        Task last = pollNow();
        assertEquals(4, last.attempt());
        assertTrue(queue.nack(last, "smtp error 4"));
        assertEquals(Status.DEAD, queue.status(id));
        clock.advance(Duration.ofHours(1));
        assertEquals(Optional.empty(), queue.poll(), "a dead task is never delivered");
        assertEquals(List.of(new DeadLetter(last, "smtp error 4")), queue.deadLetters());
    }

    @Test
    void anExpiredLeaseIsAFailureWithoutBackoff() {
        String id = queue.enqueue("t", "p");
        Task first = pollNow();
        clock.advance(Duration.ofMinutes(5));
        Task second = pollNow();
        assertEquals(2, second.attempt(), "ready as soon as the lease ended");
        clock.advance(Duration.ofMinutes(5));
        pollNow();
        clock.advance(Duration.ofMinutes(5));
        Task fourth = pollNow();
        assertEquals(4, fourth.attempt());
        clock.advance(Duration.ofMinutes(5));
        assertEquals(Status.DEAD, queue.status(id));
        assertEquals(List.of(new DeadLetter(fourth, "lease expired")), queue.deadLetters());
        assertFalse(queue.ack(first));
    }

    @Test
    void deadLettersComeInTheOrderTheTasksDied() {
        var queue = new TaskQueue(clock, new QueueConfig(Duration.ofMinutes(5), 1, Duration.ofSeconds(1),
                Duration.ofSeconds(5)));
        queue.enqueue("t", "first");
        queue.enqueue("t", "second");
        Task a = queue.poll().orElseThrow();
        Task b = queue.poll().orElseThrow();
        queue.nack(b, "b failed");
        queue.nack(a, "a failed");
        assertEquals(List.of(new DeadLetter(b, "b failed"), new DeadLetter(a, "a failed")), queue.deadLetters());
    }

    @Test
    void requeueGivesADeadTaskAFreshStart() {
        var queue = new TaskQueue(clock, new QueueConfig(Duration.ofMinutes(5), 1, Duration.ofSeconds(1),
                Duration.ofSeconds(5)));
        String id = queue.enqueue("t", "p");
        queue.nack(queue.poll().orElseThrow(), "boom");
        assertTrue(queue.requeue(id));
        assertEquals(Status.READY, queue.status(id));
        assertEquals(List.of(), queue.deadLetters());
        assertEquals(new Task(id, "t", "p", 1), queue.poll().orElseThrow());
        assertFalse(queue.requeue(id), "only a dead task can be requeued");
    }
}
