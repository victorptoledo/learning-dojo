package dojo;

import java.util.List;
import org.springframework.stereotype.Service;

/**
 * The new checkout: an orchestrated saga over the inventory, payment and shipping services. No
 * transaction spans the saga: each save commits on its own, so a crash leaves the last saved step.
 */
@Service
public class CheckoutSaga {
    private static final List<SagaStep> UNFINISHED =
            List.of(SagaStep.RESERVING, SagaStep.CHARGING, SagaStep.SHIPPING, SagaStep.COMPENSATING);

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
        ShopOrder order = orders.save(new ShopOrder(request.customerName(), request.customerName(), request.sku(),
                request.quantity(), request.totalCents(), OrderStatus.PENDING));
        SagaRecord saga = sagas.save(new SagaRecord(order.getId(), request));
        return run(saga);
    }

    /** Runs a saga on from the step it was saved at. A finished saga only reports its result. */
    public CheckoutResult resume(String sagaId) {
        return run(sagas.findById(sagaId).orElseThrow());
    }

    /** Resumes every saga that is not finished; a scheduler would call this every minute. */
    public int resumeStuck() {
        List<SagaRecord> stuck = sagas.findByStepIn(UNFINISHED);
        stuck.forEach(this::run);
        return stuck.size();
    }

    private CheckoutResult run(SagaRecord saga) {
        while (!saga.getStep().finished()) {
            try {
                switch (saga.getStep()) {
                    case RESERVING -> {
                        inventory.reserve(saga.getId(), saga.getSku(), saga.getQuantity());
                        save(saga, SagaStep.CHARGING);
                    }
                    case CHARGING -> {
                        payments.charge(saga.getId(), saga.getTotalCents());
                        save(saga, SagaStep.SHIPPING);
                    }
                    case SHIPPING -> {
                        shipping.schedule(saga.getId(), saga.getAddress());
                        ShopOrder order = order(saga);
                        order.confirm();
                        orders.save(order);
                        save(saga, SagaStep.DONE);
                    }
                    case COMPENSATING -> compensate(saga);
                    default -> throw new IllegalStateException("unexpected step " + saga.getStep());
                }
            } catch (StepFailedException failure) {
                saga.fail(failure.getMessage());
                sagas.save(saga);
            }
        }
        ShopOrder order = order(saga);
        return new CheckoutResult(order.getId(), order.getStatus(), order.getReason(), "saga");
    }

    /** Undoes, newest first, the steps that ran before the one that failed. Safe to run again. */
    private void compensate(SagaRecord saga) {
        if (saga.getFailedAt() == SagaStep.SHIPPING) {
            payments.refund(saga.getId());
        }
        if (saga.getFailedAt() == SagaStep.SHIPPING || saga.getFailedAt() == SagaStep.CHARGING) {
            inventory.release(saga.getId());
        }
        ShopOrder order = order(saga);
        order.cancel(saga.getReason());
        orders.save(order);
        save(saga, SagaStep.COMPENSATED);
    }

    private void save(SagaRecord saga, SagaStep next) {
        saga.moveTo(next);
        sagas.save(saga);
    }

    private ShopOrder order(SagaRecord saga) {
        return orders.findById(saga.getOrderId()).orElseThrow();
    }
}
