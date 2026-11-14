package dojo;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class PricingTest {
    @Test
    void subtotal_adds_every_item() {
        var order = new Order(List.of(new Item("tea", "drinks", 300, 7), new Item("mug", "kitchen", 1200, 1)));
        assertEquals(3300, Pricing.subtotal(order));
    }
}
