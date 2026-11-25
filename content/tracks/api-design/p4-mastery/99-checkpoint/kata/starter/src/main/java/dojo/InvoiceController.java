package dojo;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import java.net.URI;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** Invoices, after last month's "cleanup" refactoring. */
@RestController
@RequestMapping("/invoices")
public class InvoiceController {
    /** "Friendlier" money and dates for the dashboard (merged without review). */
    record InvoiceView(long id, String customer, double amount, String currency, String status,
                       String dueDate, String issuedAt) {
        static InvoiceView of(Invoice invoice) {
            return new InvoiceView(invoice.id(), invoice.customer(), invoice.amountCents() / 100.0,
                    invoice.currency(), invoice.status(), invoice.dueDate().atStartOfDay().toString(),
                    LocalDateTime.ofInstant(invoice.issuedAt(), ZoneId.of("Europe/Lisbon")).toString());
        }
    }

    record NewInvoice(@NotBlank String customer, @NotNull @Positive Double amount,
                      @NotNull @Pattern(regexp = "[A-Z]{3}") String currency, @NotNull LocalDate dueDate) {}

    private final InvoiceStore store;

    public InvoiceController(InvoiceStore store) {
        this.store = store;
    }

    @GetMapping
    public Map<String, List<InvoiceView>> list(@RequestAttribute(Authentication.TENANT) String tenant) {
        return Map.of("data", store.ofTenant(tenant).stream().map(InvoiceView::of).toList());
    }

    @GetMapping("/{id}")
    public InvoiceView get(@PathVariable long id) {
        return store.find(id).map(InvoiceView::of)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @PostMapping
    public ResponseEntity<InvoiceView> create(@RequestAttribute(Authentication.TENANT) String tenant,
                                              @Valid @RequestBody NewInvoice request) {
        long cents = (long) (request.amount() * 100);
        Invoice invoice = store.add(tenant, request.customer(), cents, request.currency(), request.dueDate());
        return ResponseEntity.created(URI.create("/invoices/" + invoice.id())).body(InvoiceView.of(invoice));
    }
}
