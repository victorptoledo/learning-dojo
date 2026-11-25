package dojo;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** Paying an invoice. The mobile app retries this call when the network is slow. */
@RestController
@RequestMapping("/invoices/{invoiceId}/payments")
public class PaymentController {
    record PaymentRequest(@NotNull Long amountCents) {}

    record Payment(String id, long invoiceId, long amountCents, String currency, String chargeId) {}

    private final InvoiceStore store;
    private final PaymentGateway gateway;
    private final AtomicLong ids = new AtomicLong();

    public PaymentController(InvoiceStore store, PaymentGateway gateway) {
        this.store = store;
        this.gateway = gateway;
    }

    @PostMapping
    public ResponseEntity<Payment> pay(@PathVariable long invoiceId, @Valid @RequestBody PaymentRequest request) {
        Invoice invoice = store.find(invoiceId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        String chargeId = gateway.charge(invoice.tenant(), invoice.id(), request.amountCents());
        store.save(invoice.paid());
        Payment payment = new Payment("pay_" + ids.incrementAndGet(), invoice.id(), request.amountCents(),
                invoice.currency(), chargeId);
        return ResponseEntity.created(URI.create("/invoices/" + invoiceId + "/payments/" + payment.id()))
                .body(payment);
    }
}
