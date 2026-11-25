package dojo;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Component;

/**
 * The invoices of every tenant, ordered by id. Written for you. Ids grow, so new invoices
 * always come after the ones that exist. The seeded tenant northwind has 5,000 invoices.
 */
@Component
public class InvoiceStore {
    private final ConcurrentSkipListMap<Long, Invoice> invoices = new ConcurrentSkipListMap<>();
    private final AtomicLong ids = new AtomicLong();
    private final Ticker ticker;

    public InvoiceStore(Ticker ticker) {
        this.ticker = ticker;
        for (int i = 1; i <= 5000; i++) {
            add("northwind", "Customer " + i, 1000 + i, "EUR", LocalDate.of(2026, 6, 30));
        }
        add("contoso", "Fabrikam", 99_00, "USD", LocalDate.of(2026, 7, 15));
    }

    /** Stores a new open invoice issued now. */
    public Invoice add(String tenant, String customer, long amountCents, String currency, LocalDate dueDate) {
        long id = ids.incrementAndGet();
        Invoice invoice = new Invoice(id, tenant, customer, amountCents, currency, dueDate, ticker.now(), Invoice.OPEN);
        invoices.put(id, invoice);
        return invoice;
    }

    /** Any tenant's invoice with this id. */
    public Optional<Invoice> find(long id) {
        return Optional.ofNullable(invoices.get(id));
    }

    /** Every invoice of the tenant, by id. */
    public List<Invoice> ofTenant(String tenant) {
        return invoices.values().stream().filter(invoice -> invoice.tenant().equals(tenant)).toList();
    }

    /** At most {@code limit} invoices of the tenant whose id is greater than {@code afterId}, by id. */
    public List<Invoice> ofTenantAfter(String tenant, long afterId, int limit) {
        return invoices.tailMap(afterId, false).values().stream()
                .filter(invoice -> invoice.tenant().equals(tenant))
                .limit(limit)
                .toList();
    }

    public void save(Invoice invoice) {
        invoices.put(invoice.id(), invoice);
    }
}
