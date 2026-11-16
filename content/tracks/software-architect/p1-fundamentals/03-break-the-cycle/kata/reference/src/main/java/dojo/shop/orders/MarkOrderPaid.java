package dojo.shop.orders;

import dojo.shop.billing.PaymentListener;

/** Orders' side of the port: a paid invoice marks its order paid. */
public class MarkOrderPaid implements PaymentListener {
    private final OrderRepository orders;

    public MarkOrderPaid(OrderRepository orders) {
        this.orders = orders;
    }

    @Override
    public void invoicePaid(String orderId) {
        orders.find(orderId)
                .orElseThrow(() -> new IllegalArgumentException("unknown order " + orderId))
                .markPaid();
    }
}
