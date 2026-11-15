package dojo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/**
 * Characterisation tests: they pin today's prices, so they pass before and after the
 * refactoring. Keep them green while you move the code.
 */
class ShippingCalculatorTest {
    private final ShippingCalculator calculator = new ShippingCalculator();

    @Test
    void standard_adds_per_started_kilo_above_two_and_abroad() {
        assertEquals(499, calculator.quoteCents("standard", new Parcel(1500, "PT", false)));
        assertEquals(499 + 2 * 150 + 700, calculator.quoteCents("standard", new Parcel(3200, "ES", false)));
    }

    @Test
    void express_doubles_abroad_and_fragile_costs_extra() {
        assertEquals(1299 + 2 * 250 + 300, calculator.quoteCents("express", new Parcel(1200, "PT", true)));
        assertEquals((1299 + 250) * 2, calculator.quoteCents("express", new Parcel(800, "FR", false)));
    }

    @Test
    void pickup_and_freight_have_limits() {
        assertEquals(199, calculator.quoteCents("pickup", new Parcel(9000, "PT", true)));
        assertThrows(IllegalArgumentException.class, () -> calculator.quoteCents("pickup", new Parcel(12_000, "PT", false)));
        assertEquals(4500 + 31 * 80, calculator.quoteCents("freight", new Parcel(30_500, "DE", false)));
    }

    @Test
    void unknown_methods_are_refused() {
        var error = assertThrows(IllegalArgumentException.class,
                () -> calculator.quoteCents("teleport", new Parcel(100, "PT", false)));
        assertEquals("unknown shipping method: teleport", error.getMessage());
    }
}
