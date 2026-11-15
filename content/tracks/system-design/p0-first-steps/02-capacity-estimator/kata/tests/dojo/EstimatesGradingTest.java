package dojo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class EstimatesGradingTest {
    @Test
    void requests_per_second_round_up() {
        assertEquals(2315, Estimates.requestsPerSecond(10_000_000, 20));
        assertEquals(1, Estimates.requestsPerSecond(1, 1), "one request a day still needs capacity");
        assertEquals(0, Estimates.requestsPerSecond(0, 50));
        assertEquals(1, Estimates.requestsPerSecond(86_400, 1));
        assertEquals(2, Estimates.requestsPerSecond(86_401, 1));
        assertThrows(IllegalArgumentException.class, () -> Estimates.requestsPerSecond(-1, 1));
    }

    @Test
    void the_peak_is_the_average_times_the_factor_rounded_up() {
        assertEquals(6945, Estimates.peak(2315, 3));
        assertEquals(4, Estimates.peak(3, 1.2));
        assertEquals(10, Estimates.peak(10, 1));
        assertThrows(IllegalArgumentException.class, () -> Estimates.peak(10, 0.5));
    }

    @Test
    void storage_counts_every_replica_and_refuses_to_overflow() {
        assertEquals(5_000_000L * 2_000_000 * 365 * 3, Estimates.storageBytes(5_000_000, 2_000_000, 365, 3));
        assertEquals(0, Estimates.storageBytes(0, 2_000_000, 365, 3));
        assertThrows(ArithmeticException.class, () -> Estimates.storageBytes(Long.MAX_VALUE / 2, 3, 1, 1));
        assertThrows(IllegalArgumentException.class, () -> Estimates.storageBytes(-1, 10, 1, 1));
    }

    @Test
    void servers_keep_headroom_and_one_spare() {
        assertEquals(11, Estimates.serversNeeded(7000, 1000, 70), "10 to stay under 70 %, plus a spare");
        assertEquals(8, Estimates.serversNeeded(7000, 1000, 100));
        assertEquals(2, Estimates.serversNeeded(1, 1000, 50));
        assertEquals(1, Estimates.serversNeeded(0, 1000, 50), "even with no traffic you keep the spare");
        assertThrows(IllegalArgumentException.class, () -> Estimates.serversNeeded(100, 0, 50));
        assertThrows(IllegalArgumentException.class, () -> Estimates.serversNeeded(100, 10, 0));
        assertThrows(IllegalArgumentException.class, () -> Estimates.serversNeeded(100, 10, 101));
    }

    @Test
    void littles_law_gives_the_requests_in_flight() {
        assertEquals(100, Estimates.inFlight(2000, Duration.ofMillis(50)));
        assertEquals(1400, Estimates.inFlight(7000, Duration.ofMillis(200)));
        assertEquals(1, Estimates.inFlight(3, Duration.ofMillis(1)), "a fraction of a request is still one");
        assertEquals(0, Estimates.inFlight(0, Duration.ofSeconds(1)));
    }

    @Test
    void serial_components_multiply() {
        assertEquals(0.9999 * 0.999 * 0.9995, Availability.serial(0.9999, 0.999, 0.9995), 1e-12);
        assertEquals(0.999, Availability.serial(0.999), 1e-12);
        assertThrows(IllegalArgumentException.class, Availability::serial);
        assertThrows(IllegalArgumentException.class, () -> Availability.serial(0.99, 99.9));
    }

    @Test
    void parallel_copies_multiply_the_unavailability() {
        assertEquals(0.9999, Availability.parallel(0.99, 2), 1e-12);
        assertEquals(0.999, Availability.parallel(0.9, 3), 1e-12);
        assertEquals(0.95, Availability.parallel(0.95, 1), 1e-12);
        assertThrows(IllegalArgumentException.class, () -> Availability.parallel(0.99, 0));
        assertThrows(IllegalArgumentException.class, () -> Availability.parallel(-0.1, 2));
    }

    @Test
    void downtime_per_year_for_the_usual_nines() {
        assertEquals(Duration.ofHours(8).plusMinutes(45).plusSeconds(36), Availability.downtimePerYear(0.999));
        assertEquals(Duration.ofSeconds(3154), Availability.downtimePerYear(0.9999), "about 52.6 minutes");
        assertEquals(Duration.ofDays(365), Availability.downtimePerYear(0));
        assertEquals(Duration.ZERO, Availability.downtimePerYear(1));
        assertThrows(IllegalArgumentException.class, () -> Availability.downtimePerYear(1.5));
    }
}
