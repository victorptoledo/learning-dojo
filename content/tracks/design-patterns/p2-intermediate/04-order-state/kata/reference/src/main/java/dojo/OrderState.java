package dojo;

/**
 * The state of an order. Each state is a record that carries only the data that exists in
 * that state and overrides only the transitions it allows; the rest are refused here.
 */
public sealed interface OrderState permits Created, Paid, Shipped, Delivered, Cancelled {
    /** The state's name in lower case: "created", "paid" ... */
    String name();

    default OrderState pay() {
        throw refused("pay");
    }

    default OrderState ship(String tracking) {
        throw refused("ship");
    }

    default OrderState deliver() {
        throw refused("deliver");
    }

    default OrderState cancel(String reason) {
        throw refused("cancel");
    }

    private IllegalStateException refused(String action) {
        return new IllegalStateException("cannot " + action + " an order that is " + name());
    }
}
