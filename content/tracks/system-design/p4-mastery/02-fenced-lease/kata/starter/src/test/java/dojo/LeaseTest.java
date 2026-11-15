package dojo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class LeaseTest {
    @Test
    void the_first_lease_gets_token_one_and_blocks_others() {
        var leases = new LeaseManager(Duration.ofSeconds(10), new TestClock());
        var lease = leases.tryAcquire("worker-a").orElseThrow();
        assertEquals(1, lease.token());
        assertTrue(leases.tryAcquire("worker-b").isEmpty());
    }
}
