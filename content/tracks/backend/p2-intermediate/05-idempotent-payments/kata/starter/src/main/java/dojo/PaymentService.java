package dojo;

import org.springframework.stereotype.Service;

@Service
public class PaymentService {
    private final PaymentGateway gateway;
    private final PaymentRepository payments;
    private final IdempotencyRepository keys;

    public PaymentService(PaymentGateway gateway, PaymentRepository payments, IdempotencyRepository keys) {
        this.gateway = gateway;
        this.payments = payments;
        this.keys = keys;
    }

    /** Charges at most once per key; a repeat with the same request gets the first outcome back. */
    public PaymentOutcome pay(String key, PaymentRequest request) {
        throw new UnsupportedOperationException("todo pay with key " + key);
    }
}
