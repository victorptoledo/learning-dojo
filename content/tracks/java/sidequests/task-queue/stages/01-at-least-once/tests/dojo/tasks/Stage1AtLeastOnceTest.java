package dojo.tasks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class Stage1AtLeastOnceTest {

    static final QueueConfig CONFIG =
            new QueueConfig(Duration.ofMinutes(5), 5, Duration.ofSeconds(1), Duration.ofMinutes(1));

    private final ManualClock clock = new ManualClock();
    private final TaskQueue queue = new TaskQueue(clock, CONFIG);

    @Test
    void anEmptyQueueHasNothingToPoll() {
        assertEquals(Optional.empty(), queue.poll());
    }

    @Test
    void anEnqueuedTaskIsReadyWithANewId() {
        String first = queue.enqueue("email", "to=ana");
        String second = queue.enqueue("email", "to=bia");
        assertNotEquals(first, second);
        assertEquals(Status.READY, queue.status(first));
    }

    @Test
    void pollDeliversInEnqueueOrderAsFirstAttempts() {
        String a = queue.enqueue("email", "a");
        String b = queue.enqueue("sms", "b");
        assertEquals(new Task(a, "email", "a", 1), queue.poll().orElseThrow());
        assertEquals(new Task(b, "sms", "b", 1), queue.poll().orElseThrow());
        assertEquals(Optional.empty(), queue.poll());
    }

    @Test
    void aLeasedTaskIsHiddenFromOtherPolls() {
        String id = queue.enqueue("email", "a");
        queue.poll();
        assertEquals(Status.LEASED, queue.status(id));
        clock.advance(Duration.ofMinutes(4));
        assertEquals(Optional.empty(), queue.poll());
    }

    @Test
    void ackFinishesTheTask() {
        String id = queue.enqueue("email", "a");
        Task delivery = queue.poll().orElseThrow();
        assertTrue(queue.ack(delivery));
        assertEquals(Status.DONE, queue.status(id));
        clock.advance(Duration.ofHours(1));
        assertEquals(Optional.empty(), queue.poll(), "a done task never comes back");
        assertFalse(queue.ack(delivery), "acking twice changes nothing");
    }

    @Test
    void anExpiredLeaseDeliversTheTaskAgainWithTheNextAttempt() {
        String id = queue.enqueue("email", "a");
        queue.poll();
        clock.advance(Duration.ofMinutes(5).plusSeconds(1));
        assertEquals(new Task(id, "email", "a", 2), queue.poll().orElseThrow());
    }

    @Test
    void aSlowWorkerCannotAckAfterItsLeaseRanOut() {
        String id = queue.enqueue("email", "a");
        Task slow = queue.poll().orElseThrow();
        clock.advance(Duration.ofMinutes(6));
        Task fresh = queue.poll().orElseThrow();
        assertFalse(queue.ack(slow), "the slow worker's delivery is stale");
        assertEquals(Status.LEASED, queue.status(id));
        assertTrue(queue.ack(fresh));
        assertEquals(Status.DONE, queue.status(id));
    }

    @Test
    void anExpiredLeaseIsStaleEvenBeforeAnotherPoll() {
        queue.enqueue("email", "a");
        Task late = queue.poll().orElseThrow();
        clock.advance(Duration.ofMinutes(10));
        assertFalse(queue.ack(late));
    }

    @Test
    void aNackedTaskComesBackWithTheNextAttempt() {
        String id = queue.enqueue("email", "a");
        Task first = queue.poll().orElseThrow();
        assertTrue(queue.nack(first, "smtp down"));
        assertEquals(Status.READY, queue.status(id));
        assertFalse(queue.ack(first), "a nacked delivery is no longer current");
        clock.advance(Duration.ofHours(1));
        assertEquals(new Task(id, "email", "a", 2), queue.poll().orElseThrow());
    }

    @Test
    void theTaskReadyTheLongestComesFirst() {
        String a = queue.enqueue("t", "a");
        queue.poll();
        clock.advance(Duration.ofMinutes(5));
        String b = queue.enqueue("t", "b");
        clock.advance(Duration.ofMinutes(2));
        // a has been ready again since its lease ended, before b was enqueued
        assertEquals(a, queue.poll().orElseThrow().id());
        assertEquals(b, queue.poll().orElseThrow().id());
    }

    @Test
    void anUnknownIdHasNoStatus() {
        assertThrows(NoSuchElementException.class, () -> queue.status("nope"));
    }
}
