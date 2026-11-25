package dojo;

import dojo.ApiExceptions.AlreadyPaidException;
import dojo.ApiExceptions.AmountMismatchException;
import dojo.ApiExceptions.KeyMissingException;
import dojo.ApiExceptions.KeyReusedException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Paying an invoice, safe to retry: one charge per Idempotency-Key, whatever the network does. */
@RestController
@RequestMapping("/invoices/{invoiceId}/payments")
public class PaymentController {
    record PaymentRequest(@NotNull Long amountCents) {}

    record Payment(String id, long invoiceId, long amountCents, String currency, String chargeId) {}

    private record Stored(String fingerprint, Payment payment) {}

    private final InvoiceStore store;
    private final PaymentGateway gateway;
    private final Map<String, Stored> byKey = new ConcurrentHashMap<>();
    private final AtomicLong ids = new AtomicLong();

    public PaymentController(InvoiceStore store, PaymentGateway gateway) {
        this.store = store;
        this.gateway = gateway;
    }

    @PostMapping
    public synchronized ResponseEntity<Payment> pay(
            @RequestAttribute(Authentication.TENANT) String tenant, @PathVariable long invoiceId,
            @RequestHeader(name = "Idempotency-Key", required = false) String key,
            @Valid @RequestBody PaymentRequest request) {
        if (key == null || key.isBlank()) {
            throw new KeyMissingException();
        }
        String scoped = tenant + "\n" + key;
        String fingerprint = invoiceId + ":" + request.amountCents();
        Stored stored = byKey.get(scoped);
        if (stored != null) {
            if (!stored.fingerprint().equals(fingerprint)) {
                throw new KeyReusedException();
            }
            return created(stored.payment()).header("Idempotent-Replayed", "true").body(stored.payment());
        }
        Payment payment = charge(InvoiceController.owned(store, tenant, invoiceId), request.amountCents());
        byKey.put(scoped, new Stored(fingerprint, payment));
        return created(payment).body(payment);
    }

    private Payment charge(Invoice invoice, long amountCents) {
        if (Invoice.PAID.equals(invoice.status())) {
            throw new AlreadyPaidException(invoice.id());
        }
        if (amountCents != invoice.amountCents()) {
            throw new AmountMismatchException(invoice.amountCents());
        }
        String chargeId = gateway.charge(invoice.tenant(), invoice.id(), amountCents);
        store.save(invoice.paid());
        return new Payment("pay_" + ids.incrementAndGet(), invoice.id(), amountCents, invoice.currency(), chargeId);
    }

    private static ResponseEntity.BodyBuilder created(Payment payment) {
        return ResponseEntity.created(URI.create("/invoices/" + payment.invoiceId() + "/payments/" + payment.id()));
    }
}
