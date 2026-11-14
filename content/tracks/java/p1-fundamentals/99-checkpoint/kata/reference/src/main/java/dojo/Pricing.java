package dojo;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Prices orders. Every amount is in cents. */
public final class Pricing {
    private Pricing() {}

    public static long subtotal(Order order) {
        long total = 0;
        for (Item item : order.items()) {
            total += item.subtotal();
        }
        return total;
    }

    public static long discountFor(Order order, Discount discount) {
        return switch (discount) {
            case PercentOff(var category, var percent) -> {
                long categorySubtotal = 0;
                for (Item item : order.items()) {
                    if (item.category().equals(category)) {
                        categorySubtotal += item.subtotal();
                    }
                }
                yield categorySubtotal * percent / 100;
            }
            case AmountOff(var cents, var minimum) -> subtotal(order) >= minimum ? cents : 0;
            case BuyXGetY(var sku, var buy, var free) -> {
                long off = 0;
                for (Item item : order.items()) {
                    if (item.sku().equals(sku)) {
                        long groups = item.quantity() / (buy + free);
                        off += groups * free * item.unitCents();
                    }
                }
                yield off;
            }
        };
    }

    public static long total(Order order, List<Discount> discounts) {
        long total = subtotal(order);
        for (Discount discount : discounts) {
            total -= discountFor(order, discount);
        }
        return Math.max(0, total);
    }

    public static Map<String, Long> subtotalByCategory(Order order) {
        Map<String, Long> byCategory = new TreeMap<>();
        for (Item item : order.items()) {
            byCategory.merge(item.category(), item.subtotal(), Long::sum);
        }
        return byCategory;
    }
}
