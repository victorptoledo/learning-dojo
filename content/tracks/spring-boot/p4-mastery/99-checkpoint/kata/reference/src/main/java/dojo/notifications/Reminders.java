package dojo.notifications;

import dojo.billing.InvoiceIssued;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionalEventListener;

@Service
public class Reminders {
    private final int daysBefore;
    private final List<Reminder> planned = new CopyOnWriteArrayList<>();

    public Reminders(@Value("${notifications.reminder-days-before:3}") int daysBefore) {
        this.daysBefore = daysBefore;
    }

    // Only billing's public event, and only once the invoice is committed.
    @TransactionalEventListener
    public void on(InvoiceIssued issued) {
        planned.add(new Reminder(issued.number(), issued.customer(), issued.dueOn().minusDays(daysBefore)));
    }

    public List<Reminder> planned() {
        return List.copyOf(planned);
    }
}
