package dojo;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class PaymentService {
    private final PaymentGateway gateway;
    private final PaymentRepository payments;
    private final IdempotencyRepository keys;
    private final TransactionTemplate transaction;

    public PaymentService(PaymentGateway gateway, PaymentRepository payments, IdempotencyRepository keys,
                          TransactionTemplate transaction) {
        this.gateway = gateway;
        this.payments = payments;
        this.keys = keys;
        this.transaction = transaction;
    }

    /** Charges at most once per key; a repeat with the same request gets the first outcome back. */
    public PaymentOutcome pay(String key, PaymentRequest request) {
        String fingerprint = request.fingerprint();
        IdempotencyRecord existing = keys.findById(key).orElse(null);
        if (existing == null) {
            try {
                transaction.executeWithoutResult(status -> keys.saveAndFlush(new IdempotencyRecord(key, fingerprint)));
            } catch (DataIntegrityViolationException e) {
                existing = keys.findById(key).orElseThrow(PaymentException::inProgress);
            }
        }
        if (existing != null) {
            return replay(existing, fingerprint);
        }
        String chargeId;
        try {
            chargeId = gateway.charge(request.account().strip(), request.amountCents());
        } catch (DeclinedException e) {
            transaction.executeWithoutResult(status -> {
                IdempotencyRecord record = keys.findById(key).orElseThrow();
                record.finish(HttpStatus.PAYMENT_REQUIRED.value(), null, e.getMessage());
            });
            throw PaymentException.declined(e.getMessage());
        }
        PaymentView view = transaction.execute(status -> {
            Payment payment = payments.save(new Payment(request.account().strip(), request.amountCents(), chargeId));
            keys.findById(key).orElseThrow().finish(HttpStatus.CREATED.value(), payment.getId(), null);
            return PaymentView.of(payment);
        });
        return new PaymentOutcome(view, false);
    }

    private PaymentOutcome replay(IdempotencyRecord record, String fingerprint) {
        if (!record.getFingerprint().equals(fingerprint)) {
            throw PaymentException.keyReused();
        }
        if (!record.isDone()) {
            throw PaymentException.inProgress();
        }
        if (record.getPaymentId() == null) {
            throw PaymentException.declined(record.getDetail());
        }
        return new PaymentOutcome(PaymentView.of(payments.findById(record.getPaymentId()).orElseThrow()), true);
    }
}
