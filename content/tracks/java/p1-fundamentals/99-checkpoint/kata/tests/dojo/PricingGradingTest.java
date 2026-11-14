package dojo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PricingGradingTest {
    private static Order order() {
        return new Order(List.of(
                new Item("tea", "drinks", 300, 7),
                new Item("mug", "kitchen", 1200, 1),
                new Item("juice", "drinks", 250, 2)));
    }

    @Test
    void items_strip_their_text_and_know_their_subtotal() {
        var item = new Item("  tea ", " drinks ", 300, 3);
        assertEquals("tea", item.sku());
        assertEquals("drinks", item.category());
        assertEquals(900, item.subtotal());
    }

    @Test
    void items_reject_impossible_values() {
        assertThrows(IllegalArgumentException.class, () -> new Item(" ", "drinks", 1, 1));
        assertThrows(IllegalArgumentException.class, () -> new Item(null, "drinks", 1, 1));
        assertThrows(IllegalArgumentException.class, () -> new Item("tea", "", 1, 1));
        assertThrows(IllegalArgumentException.class, () -> new Item("tea", "drinks", -1, 1));
        assertThrows(IllegalArgumentException.class, () -> new Item("tea", "drinks", 1, 0));
    }

    @Test
    void an_order_keeps_its_own_unmodifiable_copy() {
        var items = new ArrayList<>(List.of(new Item("tea", "drinks", 300, 1)));
        var order = new Order(items);
        items.add(new Item("mug", "kitchen", 1200, 1));
        assertEquals(1, order.items().size());
        var mug = new Item("mug", "kitchen", 1200, 1);
        var copied = order.items();
        assertThrows(UnsupportedOperationException.class, () -> copied.add(mug));
    }

    @Test
    void subtotal_of_an_order_and_of_an_empty_one() {
        assertEquals(3800, Pricing.subtotal(order()));
        assertEquals(0, Pricing.subtotal(new Order(List.of())));
    }

    @Test
    void percent_off_applies_to_its_category_only_and_rounds_down() {
        assertEquals(300, Pricing.discountFor(order(), new PercentOff("kitchen", 25)));
        assertEquals(858, Pricing.discountFor(order(), new PercentOff("drinks", 33)));
        assertEquals(0, Pricing.discountFor(order(), new PercentOff("toys", 50)));
    }

    @Test
    void amount_off_needs_the_minimum_subtotal() {
        assertEquals(500, Pricing.discountFor(order(), new AmountOff(500, 3800)));
        assertEquals(0, Pricing.discountFor(order(), new AmountOff(500, 3801)));
    }

    @Test
    void buy_x_get_y_counts_complete_groups() {
        assertEquals(600, Pricing.discountFor(order(), new BuyXGetY("tea", 2, 1)));
        assertEquals(0, Pricing.discountFor(order(), new BuyXGetY("juice", 2, 1)));
        assertEquals(0, Pricing.discountFor(order(), new BuyXGetY("cake", 1, 1)));
    }

    @Test
    void total_applies_every_discount() {
        var discounts = List.<Discount>of(new BuyXGetY("tea", 2, 1), new AmountOff(500, 2000));
        assertEquals(2700, Pricing.total(order(), discounts));
        assertEquals(3800, Pricing.total(order(), List.of()));
    }

    @Test
    void total_never_goes_below_zero() {
        var small = new Order(List.of(new Item("mug", "kitchen", 1200, 1)));
        var discounts = List.<Discount>of(new PercentOff("kitchen", 100), new AmountOff(500, 0));
        assertEquals(0, Pricing.total(small, discounts));
    }

    @Test
    void subtotals_by_category_are_sorted() {
        var byCategory = Pricing.subtotalByCategory(new Order(List.of(
                new Item("mug", "kitchen", 1200, 1),
                new Item("tea", "drinks", 300, 7),
                new Item("juice", "drinks", 250, 2))));
        assertEquals(Map.of("drinks", 2600L, "kitchen", 1200L), byCategory);
        assertEquals(List.of("drinks", "kitchen"), new ArrayList<>(byCategory.keySet()));
    }
}
