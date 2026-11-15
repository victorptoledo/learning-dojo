package dojo;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** Runs the pricing steps in order and tells the listeners. Knows no rule and no listener by name. */
public final class QuoteEngine {
    private final List<PricingStep> steps;
    private final List<QuoteListener> listeners = new CopyOnWriteArrayList<>();
    private int failedNotifications;

    public QuoteEngine(List<PricingStep> steps) {
        this.steps = List.copyOf(steps);
    }

    public Quote quote(QuoteRequest request) {
        Quote quote = new Quote(request.customer(), request.quantity() * request.unitCents(), List.of());
        for (PricingStep step : steps) {
            quote = step.apply(request, quote);
        }
        for (QuoteListener listener : listeners) {
            try {
                listener.quoted(request, quote);
            } catch (RuntimeException e) {
                failedNotifications++;
            }
        }
        return quote;
    }

    public Subscription subscribe(QuoteListener listener) {
        listeners.add(listener);
        return () -> listeners.remove(listener);
    }

    /** How many listener calls threw. A failing listener never breaks a quote or the others. */
    public int failedNotifications() {
        return failedNotifications;
    }
}
