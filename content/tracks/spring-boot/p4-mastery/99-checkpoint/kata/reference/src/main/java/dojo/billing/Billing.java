package dojo.billing;

import dojo.billing.internal.Invoice;
import dojo.billing.internal.InvoiceRepository;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDate;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ImportRuntimeHints(BillingRuntimeHints.class)
public class Billing {
    private final InvoiceRepository invoices;
    private final ApplicationEventPublisher events;
    private final Clock clock;
    private final BillingProperties properties;

    public Billing(InvoiceRepository invoices, ApplicationEventPublisher events, Clock clock,
            BillingProperties properties) {
        this.invoices = invoices;
        this.events = events;
        this.clock = clock;
        this.properties = properties;
    }

    @Transactional
    public InvoiceView issue(NewInvoice request) {
        LocalDate today = LocalDate.now(clock);
        Invoice invoice = invoices.save(new Invoice(request.customer().strip(), request.amountCents(), today,
                today.plusDays(properties.paymentTermDays())));
        invoice.assignNumber("%s-%04d".formatted(properties.invoicePrefix(), invoice.getId()));
        events.publishEvent(new InvoiceIssued(invoice.getNumber(), invoice.getCustomer(), invoice.getAmountCents(),
                invoice.getDueOn()));
        return InvoiceView.of(invoice);
    }

    @Transactional(readOnly = true)
    public InvoiceView find(String number) {
        return invoices.findByNumber(number).map(InvoiceView::of)
                .orElseThrow(() -> new InvoiceNotFoundException(number));
    }

    public String text(String number) {
        return formatter().format(find(number)) + "\n" + terms();
    }

    private InvoiceFormatter formatter() {
        try {
            return (InvoiceFormatter) Class.forName(properties.formatter()).getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("cannot create the invoice formatter " + properties.formatter(), e);
        }
    }

    private String terms() {
        try {
            return new ClassPathResource("billing/terms.txt").getContentAsString(StandardCharsets.UTF_8).strip();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
