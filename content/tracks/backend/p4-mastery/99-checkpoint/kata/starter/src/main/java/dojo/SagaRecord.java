package dojo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

/** The saga's state, saved after every step so that another run can pick it up where it stopped. */
@Entity
@Table(name = "checkout_sagas")
public class SagaRecord {
    /** Also the idempotency key the saga sends to every remote service. */
    @Id
    private String id;

    private long orderId;

    @Column(nullable = false)
    private String sku;

    private int quantity;

    private long totalCents;

    @Column(nullable = false)
    private String address;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SagaStep step;

    /** The step that failed, once the saga is compensating. */
    @Enumerated(EnumType.STRING)
    private SagaStep failedAt;

    private String reason;

    protected SagaRecord() {}

    public SagaRecord(long orderId, CheckoutRequest request) {
        this.id = UUID.randomUUID().toString();
        this.orderId = orderId;
        this.sku = request.sku();
        this.quantity = request.quantity();
        this.totalCents = request.totalCents();
        this.address = request.address();
        this.step = SagaStep.RESERVING;
    }

    public void moveTo(SagaStep next) {
        this.step = next;
    }

    /** The current step failed for a business reason: undo the steps before it. */
    public void fail(String reason) {
        this.failedAt = step;
        this.reason = reason;
        this.step = SagaStep.COMPENSATING;
    }

    public String getId() {
        return id;
    }

    public long getOrderId() {
        return orderId;
    }

    public String getSku() {
        return sku;
    }

    public int getQuantity() {
        return quantity;
    }

    public long getTotalCents() {
        return totalCents;
    }

    public String getAddress() {
        return address;
    }

    public SagaStep getStep() {
        return step;
    }

    public SagaStep getFailedAt() {
        return failedAt;
    }

    public String getReason() {
        return reason;
    }
}
