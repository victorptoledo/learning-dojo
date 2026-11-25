package dojo;

import org.springframework.stereotype.Service;

/** The new checkout: an orchestrated saga over the inventory, payment and shipping services. */
@Service
public class CheckoutSaga {
    private final OrderRepository orders;
    private final SagaRepository sagas;
    private final Inventory inventory;
    private final Payments payments;
    private final Shipping shipping;

    public CheckoutSaga(OrderRepository orders, SagaRepository sagas, Inventory inventory, Payments payments,
                        Shipping shipping) {
        this.orders = orders;
        this.sagas = sagas;
        this.inventory = inventory;
        this.payments = payments;
        this.shipping = shipping;
    }

    public CheckoutResult start(CheckoutRequest request) {
        throw new UnsupportedOperationException("todo start " + request);
    }

    /** Runs a saga on from the step it was saved at. A finished saga only reports its result. */
    public CheckoutResult resume(String sagaId) {
        throw new UnsupportedOperationException("todo resume " + sagaId);
    }

    /** Resumes every saga that is not finished; a scheduler would call this every minute. */
    public int resumeStuck() {
        throw new UnsupportedOperationException("todo resume the stuck sagas");
    }
}
