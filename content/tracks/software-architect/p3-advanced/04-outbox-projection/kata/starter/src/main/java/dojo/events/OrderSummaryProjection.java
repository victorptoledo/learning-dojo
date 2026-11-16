package dojo.events;

import java.util.function.Consumer;

/** A read model of open orders per customer, built from events. */
public class OrderSummaryProjection implements Consumer<Event> {
    @Override
    public void accept(Event event) {
        throw new UnsupportedOperationException("todo: apply the event once");
    }

    public CustomerSummary summary(String customerId) {
        throw new UnsupportedOperationException("todo: read the summary");
    }
}
