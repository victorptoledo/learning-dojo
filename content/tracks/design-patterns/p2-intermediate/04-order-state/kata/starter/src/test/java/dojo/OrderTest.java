package dojo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/**
 * Characterisation tests: they pin today's behaviour, so they pass before and after the
 * refactoring. Keep them green while you move the code.
 */
class OrderTest {
    @Test
    void the_happy_path_goes_from_created_to_delivered() {
        var order = new Order("o-1", 1250);
        assertEquals("o-1: created", order.describe());
        order.pay();
        assertEquals("o-1: paid 12.50", order.describe());
        order.ship("TRK-9");
        assertEquals("SHIPPED", order.status());
        order.deliver();
        assertEquals("o-1: delivered with TRK-9", order.describe());
    }

    @Test
    void cancelling_a_paid_order_refunds_it() {
        var order = new Order("o-2", 990);
        order.pay();
        order.cancel("changed my mind");
        assertEquals("o-2: cancelled (changed my mind), refunded 9.90", order.describe());
    }

    @Test
    void illegal_transitions_are_refused() {
        var order = new Order("o-3", 100);
        var error = assertThrows(IllegalStateException.class, () -> order.ship("TRK"));
        assertEquals("cannot ship an order that is created", error.getMessage());
    }
}
