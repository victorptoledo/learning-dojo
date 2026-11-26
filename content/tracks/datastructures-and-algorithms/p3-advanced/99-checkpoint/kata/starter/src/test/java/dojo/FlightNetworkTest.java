package dojo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class FlightNetworkTest {
    private static FlightNetwork example() {
        var net = new FlightNetwork();
        net.addRoute("LIS", "MAD", 40);
        net.addRoute("MAD", "PAR", 60);
        net.addRoute("LIS", "PAR", 150);
        net.addRoute("PAR", "BER", 50);
        net.addRoute("LIS", "LON", 70);
        net.addRoute("LON", "BER", 120);
        net.addAirport("OSL");
        return net;
    }

    @Test
    void the_example_of_the_readme() {
        var net = example();
        assertTrue(net.connected("BER", "LIS"));
        assertFalse(net.connected("LIS", "OSL"));
        assertEquals(150, net.cheapest("LIS", "BER"));
        assertEquals(-1, net.cheapest("BER", "LIS"));
        assertEquals(190, net.cheapestWithin("LIS", "BER", 2));
        assertEquals(-1, net.cheapestWithin("LIS", "BER", 1));
        assertEquals(List.of("LIS", "LON"), net.suggest("L", 5));
        assertEquals(-1, net.networkCost());
    }

    @Test
    void network_cost_once_every_airport_is_reachable() {
        var net = example();
        net.addRoute("OSL", "BER", 90);
        assertEquals(40 + 60 + 50 + 70 + 90, net.networkCost());
    }

    @Test
    void bad_input_is_refused() {
        var net = example();
        assertThrows(IllegalArgumentException.class, () -> net.addRoute("LIS", "LIS", 10));
        assertThrows(IllegalArgumentException.class, () -> net.addRoute("LIS", "MAD", -1));
        assertThrows(IllegalArgumentException.class, () -> net.addAirport("lis"));
        assertThrows(IllegalArgumentException.class, () -> net.cheapest("LIS", "NYC"));
    }
}
