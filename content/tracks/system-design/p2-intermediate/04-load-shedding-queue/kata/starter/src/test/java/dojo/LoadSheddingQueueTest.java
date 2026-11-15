package dojo;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class LoadSheddingQueueTest {
    @Test
    void critical_jobs_are_served_before_sheddable_ones() {
        var queue = new LoadSheddingQueue(10, 8, Duration.ofSeconds(5), new TestClock());
        queue.offer("prefetch", Priority.SHEDDABLE);
        queue.offer("checkout", Priority.CRITICAL);
        assertEquals(Optional.of("checkout"), queue.poll());
        assertEquals(Optional.of("prefetch"), queue.poll());
    }
}
