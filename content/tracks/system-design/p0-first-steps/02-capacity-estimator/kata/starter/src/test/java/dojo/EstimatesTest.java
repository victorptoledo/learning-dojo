package dojo;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class EstimatesTest {
    @Test
    void ten_million_users_with_twenty_requests_a_day() {
        assertEquals(2315, Estimates.requestsPerSecond(10_000_000, 20));
    }

    @Test
    void two_replicas_at_99_percent() {
        assertEquals(0.9999, Availability.parallel(0.99, 2), 1e-12);
    }
}
