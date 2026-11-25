package dojo;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The monolith's checkout, still serving the users outside the rollout. In the real monolith stock and
 * payments were local tables in this same transaction; here it simply confirms the order.
 */
@Service
public class LegacyCheckout {
    private final OrderRepository orders;

    public LegacyCheckout(OrderRepository orders) {
        this.orders = orders;
    }

    @Transactional
    public CheckoutResult checkout(CheckoutRequest request) {
        // Written before the migration started: it fills the old column only.
        ShopOrder order = orders.save(new ShopOrder(request.customerName(), null, request.sku(), request.quantity(),
                request.totalCents(), OrderStatus.CONFIRMED));
        return new CheckoutResult(order.getId(), order.getStatus(), null, "legacy");
    }
}
