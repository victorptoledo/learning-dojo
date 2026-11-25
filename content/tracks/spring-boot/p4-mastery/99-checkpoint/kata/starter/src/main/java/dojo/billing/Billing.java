package dojo.billing;

import dojo.billing.internal.Invoice;
import dojo.billing.internal.InvoiceRepository;
import dojo.notifications.Reminders;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** The billing module's public entry point: issue invoices and read them back. */
@Service
public class Billing {
    private final InvoiceRepository invoices;
    private final Reminders reminders;
    private final Clock clock;
    private final String invoicePrefix;
    private final int paymentTermDays;
    private final String formatter;

    public Billing(InvoiceRepository invoices, Reminders reminders, Clock clock,
            @Value("${billing.invoice-prefix:INV}") String invoicePrefix,
            @Value("${billing.payment-term-days:30}") int paymentTermDays,
            @Value("${billing.formatter:dojo.billing.internal.PlainFormatter}") String formatter) {
        this.invoices = invoices;
        this.reminders = reminders;
        this.clock = clock;
        this.invoicePrefix = invoicePrefix;
        this.paymentTermDays = paymentTermDays;
        this.formatter = formatter;
    }

    @Transactional
    public InvoiceView issue(NewInvoice request) {
        LocalDate today = LocalDate.now(clock);
        Invoice invoice = invoices.save(new Invoice(request.customer().strip(), request.amountCents(), today,
                today.plusDays(paymentTermDays)));
        invoice.assignNumber("%s-%04d".formatted(invoicePrefix, invoice.getId()));
        reminders.invoiceIssued(invoice.getNumber());
        return InvoiceView.of(invoice);
    }

    @Transactional(readOnly = true)
    public InvoiceView find(String number) {
        return invoices.findByNumber(number).map(InvoiceView::of)
                .orElseThrow(() -> new InvoiceNotFoundException(number));
    }

    /** The printable invoice: the configured formatter, then the payment terms. */
    public String text(String number) {
        return formatter().format(find(number)) + "\n" + terms();
    }

    // The formatter is chosen by class name in the configuration, so it is created by reflection.
    private InvoiceFormatter formatter() {
        try {
            return (InvoiceFormatter) Class.forName(formatter).getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("cannot create the invoice formatter " + formatter, e);
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
