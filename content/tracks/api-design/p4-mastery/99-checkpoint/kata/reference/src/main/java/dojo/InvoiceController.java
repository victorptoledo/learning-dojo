package dojo;

import dojo.ApiExceptions.InvalidCursorException;
import dojo.ApiExceptions.InvalidParameterException;
import dojo.ApiExceptions.InvoiceNotFoundException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Base64;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/** Invoices: the v1 contract restored, each tenant's own invoices only, in bounded pages. */
@RestController
@RequestMapping("/invoices")
public class InvoiceController {
    static final int DEFAULT_LIMIT = 50;
    static final int MAX_LIMIT = 200;

    /** The wire format of version 1: money in cents, a calendar day, an instant in UTC. */
    record InvoiceView(long id, String customer, long amountCents, String currency, String status,
                       LocalDate dueDate, Instant issuedAt) {
        static InvoiceView of(Invoice invoice) {
            return new InvoiceView(invoice.id(), invoice.customer(), invoice.amountCents(), invoice.currency(),
                    invoice.status(), invoice.dueDate(), invoice.issuedAt());
        }
    }

    record NewInvoice(@NotBlank String customer, @NotNull @Positive Long amountCents,
                      @NotNull @Pattern(regexp = "[A-Z]{3}") String currency, @NotNull LocalDate dueDate) {}

    record Page(List<InvoiceView> data, String nextCursor) {}

    private final InvoiceStore store;

    public InvoiceController(InvoiceStore store) {
        this.store = store;
    }

    @GetMapping
    public ResponseEntity<Page> list(@RequestAttribute(Authentication.TENANT) String tenant,
                                     @RequestParam(required = false) Integer limit,
                                     @RequestParam(required = false) String cursor) {
        int size = limit == null ? DEFAULT_LIMIT : limit;
        if (size < 1 || size > MAX_LIMIT) {
            throw new InvalidParameterException("limit must be between 1 and " + MAX_LIMIT);
        }
        List<Invoice> found = store.ofTenantAfter(tenant, cursor == null ? 0 : decode(cursor), size + 1);
        List<Invoice> page = found.subList(0, Math.min(size, found.size()));
        if (found.size() <= size) {
            return ResponseEntity.ok(new Page(page.stream().map(InvoiceView::of).toList(), null));
        }
        String next = encode(page.getLast().id());
        String link = ServletUriComponentsBuilder.fromCurrentRequest().replaceQueryParam("cursor", next).toUriString();
        return ResponseEntity.ok()
                .header(HttpHeaders.LINK, "<" + link + ">; rel=\"next\"")
                .body(new Page(page.stream().map(InvoiceView::of).toList(), next));
    }

    @GetMapping("/{id}")
    public InvoiceView get(@RequestAttribute(Authentication.TENANT) String tenant, @PathVariable long id) {
        return InvoiceView.of(owned(store, tenant, id));
    }

    @PostMapping
    public ResponseEntity<InvoiceView> create(@RequestAttribute(Authentication.TENANT) String tenant,
                                              @Valid @RequestBody NewInvoice request) {
        Invoice invoice = store.add(tenant, request.customer(), request.amountCents(), request.currency(),
                request.dueDate());
        return ResponseEntity.created(URI.create("/invoices/" + invoice.id())).body(InvoiceView.of(invoice));
    }

    /** The tenant's own invoice; another tenant's is as unknown as one that does not exist. */
    static Invoice owned(InvoiceStore store, String tenant, long id) {
        return store.find(id).filter(invoice -> invoice.tenant().equals(tenant))
                .orElseThrow(() -> new InvoiceNotFoundException(id));
    }

    static String encode(long lastId) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(("v1:" + lastId).getBytes(StandardCharsets.UTF_8));
    }

    static long decode(String cursor) {
        try {
            String text = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
            if (!text.startsWith("v1:")) {
                throw new InvalidCursorException();
            }
            return Long.parseLong(text.substring(3));
        } catch (IllegalArgumentException e) {
            throw new InvalidCursorException();
        }
    }
}
