package dojo;

import dojo.Gateway.CardDeclinedException;
import dojo.Requests.Payment;
import dojo.Requests.PaymentRequest;
import dojo.Requests.Refund;
import dojo.Requests.RefundRequest;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/**
 * POST /payments and POST /refunds, both run through the idempotency layer. Written for you:
 * the work is in Idempotency.
 */
@RestController
public class PaymentController {
    private final Gateway gateway;
    private final Idempotency idempotency;
    private final Map<String, Payment> payments = new ConcurrentHashMap<>();
    private final AtomicLong nextId = new AtomicLong(1);

    public PaymentController(Gateway gateway, Idempotency idempotency) {
        this.gateway = gateway;
        this.idempotency = idempotency;
    }

    @PostMapping("/payments")
    public ResponseEntity<?> pay(@RequestHeader("X-Api-Key") String client,
            @RequestHeader(name = "Idempotency-Key", required = false) String key,
            @Valid @RequestBody PaymentRequest request) {
        return idempotency.execute(client, key, "POST /payments", request, () -> {
            String chargeId;
            try {
                chargeId = gateway.charge(request.account(), request.amountCents());
            } catch (CardDeclinedException e) {
                ProblemDetail declined = ProblemDetail.forStatusAndDetail(HttpStatus.PAYMENT_REQUIRED, e.getMessage());
                declined.setType(URI.create(ApiProblems.TYPES + "card-declined"));
                declined.setTitle("Card declined");
                return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED).body(declined);
            }
            Payment payment = new Payment("pay_" + nextId.getAndIncrement(), request.account(),
                    request.amountCents(), chargeId);
            payments.put(payment.id(), payment);
            return ResponseEntity.created(URI.create("/payments/" + payment.id())).body(payment);
        });
    }

    @GetMapping("/payments/{id}")
    public ResponseEntity<Payment> payment(@PathVariable String id) {
        return ResponseEntity.ofNullable(payments.get(id));
    }

    @PostMapping("/refunds")
    public ResponseEntity<?> refund(@RequestHeader("X-Api-Key") String client,
            @RequestHeader(name = "Idempotency-Key", required = false) String key,
            @Valid @RequestBody RefundRequest request) {
        return idempotency.execute(client, key, "POST /refunds", request, () -> {
            String id = gateway.refund(request.paymentId(), request.amountCents());
            return ResponseEntity.status(HttpStatus.CREATED).body(new Refund(id, request.paymentId(),
                    request.amountCents()));
        });
    }
}
