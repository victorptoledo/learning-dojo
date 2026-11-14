package dojo;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
public class PaymentController {
    private final PaymentService payments;

    public PaymentController(PaymentService payments) {
        this.payments = payments;
    }

    @PostMapping("/payments")
    public ResponseEntity<PaymentView> pay(@RequestHeader("Idempotency-Key") String key,
                                           @Valid @RequestBody PaymentRequest request, UriComponentsBuilder uri) {
        PaymentOutcome outcome = payments.pay(key.strip(), request);
        PaymentView payment = outcome.payment();
        return ResponseEntity.created(uri.path("/payments/{id}").build(payment.id()))
                .header("Idempotent-Replayed", String.valueOf(outcome.replayed()))
                .body(payment);
    }
}
