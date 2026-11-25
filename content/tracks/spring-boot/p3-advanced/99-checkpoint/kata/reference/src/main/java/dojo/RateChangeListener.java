package dojo;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class RateChangeListener {
    private final AuditTrail audit;
    private final MeterRegistry meters;

    public RateChangeListener(AuditTrail audit, MeterRegistry meters) {
        this.audit = audit;
        this.meters = meters;
    }

    // The default phase is AFTER_COMMIT: a rolled-back change is never audited or counted.
    @TransactionalEventListener
    public void on(RateChanged change) {
        audit.record(change.describe());
        meters.counter("desk.rate.updates", "currency", change.code()).increment();
    }
}
