package dojo.events;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/** A read model of open orders per customer, built from events, each applied once. */
public class OrderSummaryProjection implements Consumer<Event> {
    private final Set<String> applied = new HashSet<>();
    private final Map<String, CustomerSummary> summaries = new HashMap<>();

    @Override
    public void accept(Event event) {
        if (!applied.add(event.id())) {
            return;
        }
        int sign = switch (event.type()) {
            case "OrderPlaced" -> 1;
            case "OrderCancelled" -> -1;
            default -> 0;
        };
        CustomerSummary old = summary(event.customerId());
        summaries.put(event.customerId(), new CustomerSummary(event.customerId(),
                old.openOrders() + sign, old.openAmountCents() + sign * event.amountCents()));
    }

    public CustomerSummary summary(String customerId) {
        return summaries.getOrDefault(customerId, new CustomerSummary(customerId, 0, 0));
    }
}
