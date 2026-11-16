package dojo.shop.billing;

/** A port: billing announces a paid invoice without knowing who listens. */
public interface PaymentListener {
    void invoicePaid(String orderId);
}
