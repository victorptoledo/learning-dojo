package dojo.notifications;

import dojo.billing.internal.Invoice;
import dojo.billing.internal.InvoiceRepository;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** The notifications module: plans a payment reminder for every invoice issued. */
@Service
public class Reminders {
    private final InvoiceRepository invoices;
    private final int daysBefore;
    private final List<Reminder> planned = new CopyOnWriteArrayList<>();

    public Reminders(InvoiceRepository invoices, @Value("${notifications.reminder-days-before:3}") int daysBefore) {
        this.invoices = invoices;
        this.daysBefore = daysBefore;
    }

    /** Called by billing right after it saves an invoice. */
    public void invoiceIssued(String number) {
        Invoice invoice = invoices.findByNumber(number).orElseThrow();
        planned.add(new Reminder(invoice.getNumber(), invoice.getCustomer(), invoice.getDueOn().minusDays(daysBefore)));
    }

    public List<Reminder> planned() {
        return List.copyOf(planned);
    }
}
